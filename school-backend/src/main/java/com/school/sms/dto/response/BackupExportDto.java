package com.school.sms.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A config/reference-data backup — NOT a full database dump. Scoped
 * deliberately to the small, low-churn reference tables listed in the bonus
 * round spec (school_info, system_settings, roles, departments, designations,
 * academic_years, fee_categories, exam_types, grades). Bulk operational data
 * (students, fees, marks, ...) is explicitly out of scope: this environment
 * has no safe way to shell out to mysqldump or otherwise touch the
 * filesystem outside the app's own upload directory, so a true full-DB
 * backup is left to real database tooling (mysqldump / managed backups)
 * rather than being half-implemented here.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupExportDto {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime exportedAt;
    private String scope;

    private SchoolInfoDto schoolInfo;
    private List<SystemSettingDto> systemSettings;
    private List<RoleDto> roles;
    private List<DepartmentDto> departments;
    private List<DesignationDto> designations;
    private List<AcademicYearDto> academicYears;
    private List<FeeCategoryDto> feeCategories;
    private List<ExamTypeDto> examTypes;
    private List<GradeDto> grades;
}
