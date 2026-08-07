package com.school.sms.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.sms.dto.response.BackupExportDto;
import com.school.sms.dto.response.GradeDto;
import com.school.sms.dto.response.RoleDto;
import com.school.sms.dto.response.SchoolInfoDto;
import com.school.sms.dto.response.SystemSettingDto;
import com.school.sms.entity.Grade;
import com.school.sms.entity.Role;
import com.school.sms.entity.SchoolInfo;
import com.school.sms.entity.SystemSetting;
import com.school.sms.mapper.AcademicYearMapper;
import com.school.sms.mapper.DepartmentMapper;
import com.school.sms.mapper.DesignationMapper;
import com.school.sms.mapper.ExamTypeMapper;
import com.school.sms.mapper.FeeCategoryMapper;
import com.school.sms.repository.AcademicYearRepository;
import com.school.sms.repository.DepartmentRepository;
import com.school.sms.repository.DesignationRepository;
import com.school.sms.repository.ExamTypeRepository;
import com.school.sms.repository.FeeCategoryRepository;
import com.school.sms.repository.GradeRepository;
import com.school.sms.repository.RoleRepository;
import com.school.sms.repository.SchoolInfoRepository;
import com.school.sms.repository.SystemSettingRepository;
import com.school.sms.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    private final SchoolInfoRepository schoolInfoRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final AcademicYearRepository academicYearRepository;
    private final FeeCategoryRepository feeCategoryRepository;
    private final ExamTypeRepository examTypeRepository;
    private final GradeRepository gradeRepository;

    private final DepartmentMapper departmentMapper;
    private final DesignationMapper designationMapper;
    private final AcademicYearMapper academicYearMapper;
    private final FeeCategoryMapper feeCategoryMapper;
    private final ExamTypeMapper examTypeMapper;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReferenceData() {
        BackupExportDto backup = BackupExportDto.builder()
                .exportedAt(LocalDateTime.now())
                .scope("Reference/config data only (school_info, system_settings, roles, departments, "
                        + "designations, academic_years, fee_categories, exam_types, grades) — NOT a full database dump")
                .schoolInfo(schoolInfoRepository.findById(1L).map(this::toSchoolInfoDto).orElse(null))
                .systemSettings(systemSettingRepository.findAll().stream().map(this::toSystemSettingDto).toList())
                .roles(roleRepository.findAll().stream().map(this::toRoleDto).toList())
                .departments(departmentRepository.findAllByOrderByNameAsc().stream().map(departmentMapper::toDto).toList())
                .designations(designationRepository.findAllByOrderByNameAsc().stream().map(designationMapper::toDto).toList())
                .academicYears(academicYearRepository.findAllByOrderByStartDateDesc().stream().map(academicYearMapper::toDto).toList())
                .feeCategories(feeCategoryRepository.findAllByOrderByNameAsc().stream().map(feeCategoryMapper::toDto).toList())
                .examTypes(examTypeRepository.findAllByOrderByNameAsc().stream().map(examTypeMapper::toDto).toList())
                .grades(gradeRepository.findAll().stream().map(this::toGradeDto).toList())
                .build();

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(backup);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize backup export", ex);
        }
    }

    private SchoolInfoDto toSchoolInfoDto(SchoolInfo info) {
        return SchoolInfoDto.builder()
                .id(info.getId())
                .name(info.getName())
                .address(info.getAddress())
                .phone(info.getPhone())
                .email(info.getEmail())
                .logoUrl(info.getLogoUrl())
                .establishedYear(info.getEstablishedYear())
                .affiliationNumber(info.getAffiliationNumber())
                .build();
    }

    private SystemSettingDto toSystemSettingDto(SystemSetting setting) {
        return SystemSettingDto.builder().key(setting.getSettingKey()).value(setting.getSettingValue()).build();
    }

    private RoleDto toRoleDto(Role role) {
        return RoleDto.builder().id(role.getId()).name(role.getName()).description(role.getDescription()).build();
    }

    private GradeDto toGradeDto(Grade grade) {
        return GradeDto.builder()
                .id(grade.getId())
                .gradeName(grade.getGradeName())
                .minPercentage(grade.getMinPercentage())
                .maxPercentage(grade.getMaxPercentage())
                .gradePoint(grade.getGradePoint())
                .build();
    }
}
