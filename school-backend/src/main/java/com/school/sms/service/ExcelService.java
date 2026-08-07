package com.school.sms.service;

import com.school.sms.dto.response.ExcelImportResultDto;
import org.springframework.web.multipart.MultipartFile;

public interface ExcelService {

    /** Exports the filtered student list as an .xlsx workbook (admission no., name, class, section, roll no., status, guardian name/phone). */
    byte[] exportStudents(Long classId, Long sectionId, String status);

    /**
     * Bulk-creates students from an uploaded .xlsx (name, class, section, roll no., status,
     * guardian name/phone — admission number is server-generated). Runs inside a single
     * transaction; rows that fail validation are skipped and reported rather than aborting
     * the whole batch.
     */
    ExcelImportResultDto importStudents(MultipartFile file);

    /** Exports the full teacher list as an .xlsx workbook. */
    byte[] exportTeachers();

    /** Exports the fee-collection report's byCategory/byMonth breakdown as an .xlsx workbook. */
    byte[] exportFeeCollectionReport(Long academicYearId);
}
