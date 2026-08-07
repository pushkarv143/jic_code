package com.school.sms.controller;

import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.AttendanceSummaryReportDto;
import com.school.sms.dto.response.FeeCollectionReportDto;
import com.school.sms.dto.response.LibrarySummaryReportDto;
import com.school.sms.dto.response.PayrollSummaryReportDto;
import com.school.sms.dto.response.StudentsSummaryDto;
import com.school.sms.dto.response.TeachersSummaryDto;
import com.school.sms.dto.response.TransportSummaryReportDto;
import com.school.sms.service.ExcelService;
import com.school.sms.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reports", description = "Cross-module aggregate reports for management")
public class ReportController {

    private final ReportService reportService;
    private final ExcelService excelService;

    private static final String CORE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";
    private static final String FINANCE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','ACCOUNTANT')";

    @GetMapping("/students-summary")
    @PreAuthorize(CORE_ROLES)
    @Operation(summary = "Active student totals, broken down by class and by status")
    public ResponseEntity<ApiResponse<StudentsSummaryDto>> getStudentsSummary() {
        return ResponseEntity.ok(ApiResponse.success("Students summary retrieved successfully",
                reportService.getStudentsSummary()));
    }

    @GetMapping("/teachers-summary")
    @PreAuthorize(CORE_ROLES)
    @Operation(summary = "Active teacher totals, broken down by department")
    public ResponseEntity<ApiResponse<TeachersSummaryDto>> getTeachersSummary() {
        return ResponseEntity.ok(ApiResponse.success("Teachers summary retrieved successfully",
                reportService.getTeachersSummary()));
    }

    @GetMapping("/attendance-summary")
    @PreAuthorize(CORE_ROLES)
    @Operation(summary = "School-wide (or single-class) average attendance percentage over a date range")
    public ResponseEntity<ApiResponse<AttendanceSummaryReportDto>> getAttendanceSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long classId) {
        return ResponseEntity.ok(ApiResponse.success("Attendance summary retrieved successfully",
                reportService.getAttendanceSummary(startDate, endDate, classId)));
    }

    @GetMapping("/fee-collection")
    @PreAuthorize(FINANCE_ROLES)
    @Operation(summary = "Fee due/collected/outstanding totals, broken down by category and by month")
    public ResponseEntity<ApiResponse<FeeCollectionReportDto>> getFeeCollectionReport(
            @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Fee collection report retrieved successfully",
                reportService.getFeeCollectionReport(academicYearId)));
    }

    @GetMapping("/payroll-summary")
    @PreAuthorize(FINANCE_ROLES)
    @Operation(summary = "Payroll paid/pending totals for a year, broken down by month")
    public ResponseEntity<ApiResponse<PayrollSummaryReportDto>> getPayrollSummary(@RequestParam Integer year) {
        return ResponseEntity.ok(ApiResponse.success("Payroll summary retrieved successfully",
                reportService.getPayrollSummary(year)));
    }

    @GetMapping("/library-summary")
    @PreAuthorize(CORE_ROLES)
    @Operation(summary = "Library totals (books/issued/overdue), broken down by category")
    public ResponseEntity<ApiResponse<LibrarySummaryReportDto>> getLibrarySummary() {
        return ResponseEntity.ok(ApiResponse.success("Library summary retrieved successfully",
                reportService.getLibrarySummary()));
    }

    @GetMapping("/transport-summary")
    @PreAuthorize(CORE_ROLES)
    @Operation(summary = "Transport totals: buses, routes, and students using transport")
    public ResponseEntity<ApiResponse<TransportSummaryReportDto>> getTransportSummary() {
        return ResponseEntity.ok(ApiResponse.success("Transport summary retrieved successfully",
                reportService.getTransportSummary()));
    }

    @GetMapping("/fee-collection/export/excel")
    @PreAuthorize(FINANCE_ROLES)
    @Operation(summary = "Export the fee-collection report's byCategory/byMonth breakdown as an Excel (.xlsx) workbook")
    public ResponseEntity<byte[]> exportFeeCollectionExcel(@RequestParam(required = false) Long academicYearId) {
        byte[] workbook = excelService.exportFeeCollectionReport(academicYearId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("fee-collection-report.xlsx").build().toString())
                .body(workbook);
    }
}
