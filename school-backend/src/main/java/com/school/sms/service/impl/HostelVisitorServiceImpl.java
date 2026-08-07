package com.school.sms.service.impl;

import com.school.sms.dto.request.HostelVisitorRequest;
import com.school.sms.dto.response.HostelVisitorDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.entity.HostelVisitor;
import com.school.sms.entity.Student;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.HostelVisitorRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.security.StudentAccessGuard;
import com.school.sms.service.HostelVisitorService;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HostelVisitorServiceImpl implements HostelVisitorService {

    private final HostelVisitorRepository hostelVisitorRepository;
    private final StudentRepository studentRepository;
    private final StudentAccessGuard studentAccessGuard;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<HostelVisitorDto> getAll(Long studentId, Pageable pageable) {
        List<Long> ownIds = studentAccessGuard.resolveViewableStudentIds();
        if (ownIds != null && studentId != null && !ownIds.contains(studentId)) {
            throw new org.springframework.security.access.AccessDeniedException("You may only view your own visitor records");
        }

        Specification<HostelVisitor> spec = new SpecificationBuilder<HostelVisitor>()
                .with(studentId != null, "student.id", SearchOperation.EQUALS, studentId)
                .with(studentId == null && ownIds != null, "student.id", SearchOperation.IN, ownIds)
                .build();

        Page<HostelVisitor> page = hostelVisitorRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toDto).toList());
    }

    @Override
    @Transactional
    public HostelVisitorDto checkIn(HostelVisitorRequest request) {
        Student student = findStudent(request.getStudentId());

        HostelVisitor visitor = HostelVisitor.builder()
                .student(student)
                .visitorName(request.getVisitorName())
                .relation(request.getRelation())
                .phone(request.getPhone())
                .visitDate(request.getVisitDate() != null ? request.getVisitDate() : LocalDate.now())
                .purpose(request.getPurpose())
                .checkIn(LocalDateTime.now())
                .build();

        return toDto(hostelVisitorRepository.save(visitor));
    }

    @Override
    @Transactional
    public HostelVisitorDto checkOut(Long id) {
        HostelVisitor visitor = findEntity(id);
        if (visitor.getCheckOut() != null) {
            throw new BadRequestException("This visitor has already been checked out");
        }
        visitor.setCheckOut(LocalDateTime.now());
        return toDto(hostelVisitorRepository.save(visitor));
    }

    private HostelVisitorDto toDto(HostelVisitor visitor) {
        Student student = visitor.getStudent();
        User user = student.getUser();

        return HostelVisitorDto.builder()
                .id(visitor.getId())
                .studentId(student.getId())
                .studentName(user != null ? NameUtil.fullName(user.getFirstName(), user.getLastName()) : null)
                .visitorName(visitor.getVisitorName())
                .relation(visitor.getRelation())
                .phone(visitor.getPhone())
                .visitDate(visitor.getVisitDate())
                .purpose(visitor.getPurpose())
                .checkIn(visitor.getCheckIn())
                .checkOut(visitor.getCheckOut())
                .build();
    }

    private HostelVisitor findEntity(Long id) {
        return hostelVisitorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HostelVisitor", "id", id));
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
    }
}
