package com.school.sms.service.impl;

import com.school.sms.dto.response.GlobalSearchResultDto;
import com.school.sms.dto.response.SearchResultItemDto;
import com.school.sms.entity.Book;
import com.school.sms.entity.Student;
import com.school.sms.entity.Teacher;
import com.school.sms.repository.BookRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.service.GlobalSearchService;
import com.school.sms.util.NameUtil;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GlobalSearchServiceImpl implements GlobalSearchService {

    private static final int MAX_RESULTS_PER_CATEGORY = 5;

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final BookRepository bookRepository;

    @Override
    @Transactional(readOnly = true)
    public GlobalSearchResultDto search(String query) {
        if (!StringUtils.hasText(query)) {
            return GlobalSearchResultDto.builder().students(List.of()).teachers(List.of()).books(List.of()).build();
        }
        String term = query.trim().toLowerCase();

        return GlobalSearchResultDto.builder()
                .students(searchStudents(term))
                .teachers(searchTeachers(term))
                .books(searchBooks(term))
                .build();
    }

    private List<SearchResultItemDto> searchStudents(String term) {
        Specification<Student> spec = (root, criteriaQuery, cb) -> cb.and(
                cb.isFalse(root.get("deleted")),
                cb.or(
                        cb.like(cb.lower(root.get("admissionNumber")), "%" + term + "%"),
                        cb.like(cb.lower(root.join("user", JoinType.LEFT).get("firstName")), "%" + term + "%"),
                        cb.like(cb.lower(root.join("user", JoinType.LEFT).get("lastName")), "%" + term + "%")
                )
        );

        return studentRepository.findAll(spec, PageRequest.of(0, MAX_RESULTS_PER_CATEGORY)).getContent().stream()
                .map(student -> SearchResultItemDto.builder()
                        .id(student.getId())
                        .title(student.getUser() != null
                                ? NameUtil.fullName(student.getUser().getFirstName(), student.getUser().getLastName())
                                : student.getAdmissionNumber())
                        .subtitle(student.getSchoolClass().getClassName() + " - " + student.getSection().getSectionName())
                        .identifier(student.getAdmissionNumber())
                        .build())
                .toList();
    }

    private List<SearchResultItemDto> searchTeachers(String term) {
        Specification<Teacher> spec = (root, criteriaQuery, cb) -> cb.and(
                cb.isFalse(root.get("deleted")),
                cb.or(
                        cb.like(cb.lower(root.get("employeeId")), "%" + term + "%"),
                        cb.like(cb.lower(root.join("user", JoinType.LEFT).get("firstName")), "%" + term + "%"),
                        cb.like(cb.lower(root.join("user", JoinType.LEFT).get("lastName")), "%" + term + "%")
                )
        );

        return teacherRepository.findAll(spec, PageRequest.of(0, MAX_RESULTS_PER_CATEGORY)).getContent().stream()
                .map(teacher -> SearchResultItemDto.builder()
                        .id(teacher.getId())
                        .title(teacher.getUser() != null
                                ? NameUtil.fullName(teacher.getUser().getFirstName(), teacher.getUser().getLastName())
                                : teacher.getEmployeeId())
                        .subtitle(teacher.getDepartment().getName() + " - " + teacher.getDesignation().getName())
                        .identifier(teacher.getEmployeeId())
                        .build())
                .toList();
    }

    private List<SearchResultItemDto> searchBooks(String term) {
        Specification<Book> spec = (root, criteriaQuery, cb) -> cb.and(
                cb.isFalse(root.get("deleted")),
                cb.or(
                        cb.like(cb.lower(root.get("title")), "%" + term + "%"),
                        cb.like(cb.lower(root.get("author")), "%" + term + "%"),
                        cb.like(cb.lower(root.get("isbn")), "%" + term + "%")
                )
        );

        return bookRepository.findAll(spec, PageRequest.of(0, MAX_RESULTS_PER_CATEGORY)).getContent().stream()
                .map(book -> SearchResultItemDto.builder()
                        .id(book.getId())
                        .title(book.getTitle())
                        .subtitle(book.getAuthor())
                        .identifier(book.getIsbn())
                        .build())
                .toList();
    }
}
