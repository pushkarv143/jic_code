package com.school.sms.service.impl;

import com.school.sms.dto.request.HostelFeeRequest;
import com.school.sms.dto.response.HostelFeeDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.entity.HostelFee;
import com.school.sms.entity.HostelFeePaidStatus;
import com.school.sms.entity.Student;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.HostelFeeRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.security.StudentAccessGuard;
import com.school.sms.service.HostelFeeService;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HostelFeeServiceImpl implements HostelFeeService {

    private final HostelFeeRepository hostelFeeRepository;
    private final StudentRepository studentRepository;
    private final StudentAccessGuard studentAccessGuard;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<HostelFeeDto> getAll(Long studentId, Integer month, Integer year, String paidStatus, Pageable pageable) {
        List<Long> ownIds = studentAccessGuard.resolveViewableStudentIds();
        if (ownIds != null && studentId != null && !ownIds.contains(studentId)) {
            throw new org.springframework.security.access.AccessDeniedException("You may only view your own hostel fee records");
        }

        Specification<HostelFee> spec = new SpecificationBuilder<HostelFee>()
                .with(studentId != null, "student.id", SearchOperation.EQUALS, studentId)
                .with(studentId == null && ownIds != null, "student.id", SearchOperation.IN, ownIds)
                .with(month != null, "month", SearchOperation.EQUALS, month)
                .with(year != null, "year", SearchOperation.EQUALS, year)
                .with(StringUtils.hasText(paidStatus), "paidStatus", SearchOperation.EQUALS,
                        StringUtils.hasText(paidStatus) ? HostelFeePaidStatus.valueOf(paidStatus.toUpperCase()) : null)
                .build();

        Page<HostelFee> page = hostelFeeRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toDto).toList());
    }

    @Override
    @Transactional
    public HostelFeeDto create(HostelFeeRequest request) {
        Student student = findStudent(request.getStudentId());

        if (hostelFeeRepository.existsByStudentIdAndMonthAndYear(request.getStudentId(), request.getMonth(), request.getYear())) {
            throw new DuplicateResourceException(
                    "A hostel fee charge for this student/month/year already exists");
        }

        HostelFee fee = HostelFee.builder()
                .student(student)
                .month(request.getMonth())
                .year(request.getYear())
                .amount(request.getAmount())
                .paidStatus(HostelFeePaidStatus.UNPAID)
                .build();

        return toDto(hostelFeeRepository.save(fee));
    }

    @Override
    @Transactional
    public HostelFeeDto markPaid(Long id) {
        HostelFee fee = findEntity(id);
        if (fee.getPaidStatus() == HostelFeePaidStatus.PAID) {
            throw new BadRequestException("This hostel fee has already been marked as paid");
        }
        fee.setPaidStatus(HostelFeePaidStatus.PAID);
        return toDto(hostelFeeRepository.save(fee));
    }

    private HostelFeeDto toDto(HostelFee fee) {
        Student student = fee.getStudent();
        User user = student.getUser();

        return HostelFeeDto.builder()
                .id(fee.getId())
                .studentId(student.getId())
                .studentName(user != null ? NameUtil.fullName(user.getFirstName(), user.getLastName()) : null)
                .month(fee.getMonth())
                .year(fee.getYear())
                .amount(fee.getAmount())
                .paidStatus(fee.getPaidStatus().name())
                .build();
    }

    private HostelFee findEntity(Long id) {
        return hostelFeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HostelFee", "id", id));
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
    }
}
