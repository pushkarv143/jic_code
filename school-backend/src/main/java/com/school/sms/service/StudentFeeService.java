package com.school.sms.service;

import com.school.sms.dto.request.GenerateStudentFeesRequest;
import com.school.sms.dto.response.FeesDuesSummaryDto;
import com.school.sms.dto.response.GenerateFeesResultDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudentFeeDto;
import org.springframework.data.domain.Pageable;

public interface StudentFeeService {

    PageResponse<StudentFeeDto> getAll(Long studentId, Long classId, Long sectionId, String status,
                                        Long academicYearId, Pageable pageable);

    StudentFeeDto getById(Long id);

    GenerateFeesResultDto generate(GenerateStudentFeesRequest request);

    FeesDuesSummaryDto getDuesSummary(Long classId, Long academicYearId);
}
