package com.school.sms.service.impl;

import com.school.sms.dto.request.BookIssueRequest;
import com.school.sms.dto.request.BookReturnRequest;
import com.school.sms.dto.response.BookIssueDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.entity.Book;
import com.school.sms.entity.BookIssue;
import com.school.sms.entity.IssueStatus;
import com.school.sms.entity.Student;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.BookIssueRepository;
import com.school.sms.repository.BookRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.security.StudentAccessGuard;
import com.school.sms.service.BookIssueService;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookIssueServiceImpl implements BookIssueService {

    private static final BigDecimal FINE_PER_DAY = BigDecimal.valueOf(2);

    private final BookIssueRepository bookIssueRepository;
    private final BookRepository bookRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentAccessGuard studentAccessGuard;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookIssueDto> getAll(Long bookId, Long studentId, Long teacherId, String status, Pageable pageable) {
        List<Long> ownIds = studentAccessGuard.resolveViewableStudentIds();
        if (ownIds != null && studentId != null && !ownIds.contains(studentId)) {
            throw new org.springframework.security.access.AccessDeniedException("You may only view your own book issues");
        }

        Specification<BookIssue> spec = new SpecificationBuilder<BookIssue>()
                .with(bookId != null, "book.id", SearchOperation.EQUALS, bookId)
                .with(studentId != null, "student.id", SearchOperation.EQUALS, studentId)
                .with(studentId == null && ownIds != null, "student.id", SearchOperation.IN, ownIds)
                .with(teacherId != null, "teacher.id", SearchOperation.EQUALS, teacherId)
                .with(StringUtils.hasText(status), "status", SearchOperation.EQUALS,
                        StringUtils.hasText(status) ? IssueStatus.valueOf(status.toUpperCase()) : null)
                .build();

        Page<BookIssue> page = bookIssueRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toDto).toList());
    }

    @Override
    @Transactional
    public BookIssueDto issue(BookIssueRequest request) {
        boolean hasStudent = request.getStudentId() != null;
        boolean hasTeacher = request.getTeacherId() != null;
        if (hasStudent == hasTeacher) {
            throw new BadRequestException("Exactly one of studentId or teacherId must be provided");
        }

        Book book = bookRepository.findById(request.getBookId())
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", request.getBookId()));

        // The database trigger trg_book_issues_before_insert would also reject
        // this (SIGNAL SQLSTATE), but we check here first so the caller gets a
        // clean 400 instead of an ugly SQL exception.
        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            throw new BadRequestException("No copies of this book are currently available");
        }

        Student student = null;
        Teacher teacher = null;
        if (hasStudent) {
            student = studentRepository.findById(request.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.getStudentId()));
        } else {
            teacher = teacherRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.getTeacherId()));
        }

        BookIssue bookIssue = BookIssue.builder()
                .book(book)
                .student(student)
                .teacher(teacher)
                .issueDate(LocalDate.now())
                .dueDate(request.getDueDate())
                .fineAmount(BigDecimal.ZERO)
                .status(IssueStatus.ISSUED)
                .build();

        // trg_book_issues_after_insert (see database/04_triggers.sql) decrements
        // books.available_copies as soon as this INSERT executes.
        BookIssue saved = bookIssueRepository.saveAndFlush(bookIssue);
        entityManager.refresh(book);

        return toDto(saved);
    }

    @Override
    @Transactional
    public BookIssueDto returnBook(Long id, BookReturnRequest request) {
        BookIssue bookIssue = findEntity(id);
        if (bookIssue.getReturnDate() != null) {
            throw new BadRequestException("This book has already been returned");
        }

        LocalDate returnDate = request != null && request.getReturnDate() != null
                ? request.getReturnDate() : LocalDate.now();

        long overdueDays = ChronoUnit.DAYS.between(bookIssue.getDueDate(), returnDate);
        BigDecimal fine = overdueDays > 0 ? FINE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays)) : BigDecimal.ZERO;

        bookIssue.setReturnDate(returnDate);
        bookIssue.setFineAmount(fine);
        bookIssue.setStatus(IssueStatus.RETURNED);

        // trg_book_issues_after_update (see database/04_triggers.sql) increments
        // books.available_copies the moment return_date transitions NULL -> NOT NULL.
        BookIssue saved = bookIssueRepository.saveAndFlush(bookIssue);
        entityManager.refresh(saved.getBook());

        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookIssueDto> getOverdue(Pageable pageable) {
        Specification<BookIssue> spec = new SpecificationBuilder<BookIssue>()
                .with("status", SearchOperation.EQUALS, IssueStatus.ISSUED)
                .with("dueDate", SearchOperation.LESS_THAN, LocalDate.now())
                .build();

        Page<BookIssue> page = bookIssueRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toDto).toList());
    }

    private BookIssueDto toDto(BookIssue issue) {
        String borrowerType = null;
        String borrowerName = null;
        Long studentId = null;
        Long teacherId = null;
        String studentName = null;
        String teacherName = null;

        if (issue.getStudent() != null) {
            Student student = issue.getStudent();
            User user = student.getUser();
            studentId = student.getId();
            borrowerType = "STUDENT";
            borrowerName = user != null ? NameUtil.fullName(user.getFirstName(), user.getLastName()) : null;
            studentName = borrowerName;
        } else if (issue.getTeacher() != null) {
            Teacher teacher = issue.getTeacher();
            User user = teacher.getUser();
            teacherId = teacher.getId();
            borrowerType = "TEACHER";
            borrowerName = user != null ? NameUtil.fullName(user.getFirstName(), user.getLastName()) : null;
            teacherName = borrowerName;
        }

        return BookIssueDto.builder()
                .id(issue.getId())
                .bookId(issue.getBook().getId())
                .bookTitle(issue.getBook().getTitle())
                .studentId(studentId)
                .studentName(studentName)
                .teacherId(teacherId)
                .teacherName(teacherName)
                .borrowerType(borrowerType)
                .borrowerName(borrowerName)
                .issueDate(issue.getIssueDate())
                .dueDate(issue.getDueDate())
                .returnDate(issue.getReturnDate())
                .fineAmount(issue.getFineAmount())
                .status(issue.getStatus().name())
                .build();
    }

    private BookIssue findEntity(Long id) {
        return bookIssueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookIssue", "id", id));
    }
}
