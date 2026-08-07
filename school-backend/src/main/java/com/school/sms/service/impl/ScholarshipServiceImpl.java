package com.school.sms.service.impl;

import com.school.sms.dto.request.ScholarshipRequest;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.ScholarshipDto;
import com.school.sms.entity.AcademicYear;
import com.school.sms.entity.Scholarship;
import com.school.sms.entity.Student;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.ScholarshipMapper;
import com.school.sms.repository.AcademicYearRepository;
import com.school.sms.repository.ScholarshipRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.security.SecurityUtils;
import com.school.sms.service.ScholarshipService;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScholarshipServiceImpl implements ScholarshipService {

    private final ScholarshipRepository scholarshipRepository;
    private final StudentRepository studentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final UserRepository userRepository;
    private final ScholarshipMapper scholarshipMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ScholarshipDto> getAll(Long studentId, Long academicYearId, Pageable pageable) {
        Specification<Scholarship> spec = new SpecificationBuilder<Scholarship>()
                .with(studentId != null, "student.id", SearchOperation.EQUALS, studentId)
                .with(academicYearId != null, "academicYear.id", SearchOperation.EQUALS, academicYearId)
                .build();

        Page<Scholarship> page = scholarshipRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toDto).toList());
    }

    @Override
    @Transactional
    public ScholarshipDto create(ScholarshipRequest request) {
        Student student = findStudent(request.getStudentId());
        AcademicYear academicYear = findAcademicYear(request.getAcademicYearId());

        Scholarship entity = Scholarship.builder()
                .student(student)
                .title(request.getTitle())
                .amount(request.getAmount())
                .type(request.getType())
                .academicYear(academicYear)
                .approvedBy(SecurityUtils.getCurrentUserId())
                .build();

        return toDto(scholarshipRepository.save(entity));
    }

    @Override
    @Transactional
    public ScholarshipDto update(Long id, ScholarshipRequest request) {
        Scholarship entity = findEntity(id);
        Student student = findStudent(request.getStudentId());
        AcademicYear academicYear = findAcademicYear(request.getAcademicYearId());

        entity.setStudent(student);
        entity.setTitle(request.getTitle());
        entity.setAmount(request.getAmount());
        entity.setType(request.getType());
        entity.setAcademicYear(academicYear);

        return toDto(scholarshipRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        scholarshipRepository.delete(findEntity(id));
    }

    private ScholarshipDto toDto(Scholarship scholarship) {
        ScholarshipDto dto = scholarshipMapper.toDto(scholarship);
        var user = scholarship.getStudent().getUser();
        dto.setStudentName(user != null ? NameUtil.fullName(user.getFirstName(), user.getLastName()) : null);
        if (scholarship.getApprovedBy() != null) {
            userRepository.findById(scholarship.getApprovedBy())
                    .ifPresent(u -> dto.setApprovedByName(NameUtil.fullName(u.getFirstName(), u.getLastName())));
        }
        return dto;
    }

    private Scholarship findEntity(Long id) {
        return scholarshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scholarship", "id", id));
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
    }

    private AcademicYear findAcademicYear(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id));
    }
}
