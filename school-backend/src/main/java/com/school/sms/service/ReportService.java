package com.school.sms.service;

import com.school.sms.dto.response.AttendanceSummaryReportDto;
import com.school.sms.dto.response.FeeCollectionReportDto;
import com.school.sms.dto.response.LibrarySummaryReportDto;
import com.school.sms.dto.response.PayrollSummaryReportDto;
import com.school.sms.dto.response.StudentsSummaryDto;
import com.school.sms.dto.response.TeachersSummaryDto;
import com.school.sms.dto.response.TransportSummaryReportDto;

import java.time.LocalDate;

public interface ReportService {

    StudentsSummaryDto getStudentsSummary();

    TeachersSummaryDto getTeachersSummary();

    AttendanceSummaryReportDto getAttendanceSummary(LocalDate startDate, LocalDate endDate, Long classId);

    FeeCollectionReportDto getFeeCollectionReport(Long academicYearId);

    PayrollSummaryReportDto getPayrollSummary(Integer year);

    LibrarySummaryReportDto getLibrarySummary();

    TransportSummaryReportDto getTransportSummary();
}
