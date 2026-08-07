package com.school.sms.service.impl;

import com.school.sms.dto.response.ChildDto;
import com.school.sms.entity.Student;
import com.school.sms.entity.User;
import com.school.sms.repository.StudentRepository;
import com.school.sms.security.SecurityUtils;
import com.school.sms.service.ParentPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParentPortalServiceImpl implements ParentPortalService {

    private final StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChildDto> getMyChildren() {
        // Reuses the native-query join already established by
        // StudentAccessGuard/FeePaymentServiceImpl for PARENT scoping — no new
        // query or Parent/StudentParent entity needed.
        List<Student> children = studentRepository.findAllByParentUserId(SecurityUtils.getCurrentUserId());
        return children.stream().map(this::toDto).toList();
    }

    private ChildDto toDto(Student student) {
        User user = student.getUser();
        return ChildDto.builder()
                .studentId(student.getId())
                .firstName(user != null ? user.getFirstName() : null)
                .lastName(user != null ? user.getLastName() : null)
                .admissionNumber(student.getAdmissionNumber())
                .className(student.getSchoolClass().getClassName())
                .sectionName(student.getSection().getSectionName())
                .photoUrl(student.getPhotoUrl())
                .build();
    }
}
