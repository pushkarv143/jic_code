package com.school.sms.service;

import com.school.sms.dto.request.MarkStudentAttendanceRequest;
import com.school.sms.dto.response.MonthlyAttendanceRowDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudentAttendanceRecordDto;
import com.school.sms.dto.response.StudentAttendanceRowDto;
import com.school.sms.dto.response.StudentAttendanceSummaryDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface StudentAttendanceService {

    List<StudentAttendanceRowDto> getGrid(Long classId, Long sectionId, LocalDate date);

    int mark(MarkStudentAttendanceRequest request);

    PageResponse<StudentAttendanceRecordDto> getReport(Long studentId, Long classId, Long sectionId,
                                                        LocalDate startDate, LocalDate endDate, Pageable pageable);

    StudentAttendanceSummaryDto getSummary(Long studentId, LocalDate startDate, LocalDate endDate);

    List<MonthlyAttendanceRowDto> getMonthly(Long classId, Long sectionId, int year, int month);
}
