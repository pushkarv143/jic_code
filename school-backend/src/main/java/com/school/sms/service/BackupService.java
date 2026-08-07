package com.school.sms.service;

public interface BackupService {

    /**
     * Serializes every reference/config table (school_info, system_settings,
     * roles, departments, designations, academic_years, fee_categories,
     * exam_types, grades) into a single downloadable JSON document. See
     * {@link com.school.sms.dto.response.BackupExportDto} for why this is
     * scoped to reference data only, not a full database dump.
     */
    byte[] exportReferenceData();
}
