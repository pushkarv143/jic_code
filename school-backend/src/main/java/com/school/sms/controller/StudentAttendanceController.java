package com.school.sms.controller;

import com.school.sms.dto.request.MarkStudentAttendanceRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.MonthlyAttendanceRowDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudentAttendanceRecordDto;
import com.school.sms.dto.response.StudentAttendanceRowDto;
import com.school.sms.dto.response.StudentAttendanceSummaryDto;
import com.school.sms.service.StudentAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/attendance/students")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Student Attendance", description = "Mark and report on daily student attendance")
public class StudentAttendanceController {

    private final StudentAttendanceService studentAttendanceService;

    private static final String STAFF_READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','CLASS_TEACHER','RECEPTIONIST','ACCOUNTANT')";
    private static final String MARK_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','CLASS_TEACHER')";
    // /report and /summary are also opened to STUDENT/PARENT; the service layer
    // (StudentAccessGuard) restricts them to viewing only their own record.
    private static final String REPORT_READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','CLASS_TEACHER','RECEPTIONIST','ACCOUNTANT','STUDENT','PARENT')";

    @GetMapping
    @PreAuthorize(STAFF_READ_ROLES)
    @Operation(summary = "Get the attendance-marking grid for a class/section/date (null status/remarks = not yet marked)")
    public ResponseEntity<ApiResponse<List<StudentAttendanceRowDto>>> getGrid(
            @RequestParam Long classId,
            @RequestParam Long sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success("Attendance grid retrieved successfully",
                studentAttendanceService.getGrid(classId, sectionId, date)));
    }

    @PostMapping("/mark")
    @PreAuthorize(MARK_ROLES)
    @Operation(summary = "Mark (upsert) student attendance for a class/section/date in one batch")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> mark(@Valid @RequestBody MarkStudentAttendanceRequest request) {
        int count = studentAttendanceService.mark(request);
        return ResponseEntity.ok(ApiResponse.success(count + " student(s) marked successfully", Map.of("marked", count)));
    }

    @GetMapping("/report")
    @PreAuthorize(REPORT_READ_ROLES)
    @Operation(summary = "Paginated attendance records, filterable by student/class/section/date range")
    public ResponseEntity<ApiResponse<PageResponse<StudentAttendanceRecordDto>>> getReport(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Attendance report retrieved successfully",
                studentAttendanceService.getReport(studentId, classId, sectionId, startDate, endDate, pageable)));
    }

    @GetMapping("/{studentId}/summary")
    @PreAuthorize(REPORT_READ_ROLES)
    @Operation(summary = "Attendance percentage/day-count summary for a student over a date range")
    public ResponseEntity<ApiResponse<StudentAttendanceSummaryDto>> getSummary(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success("Attendance summary retrieved successfully",
                studentAttendanceService.getSummary(studentId, startDate, endDate)));
    }

    @GetMapping("/monthly")
    @PreAuthorize(STAFF_READ_ROLES)
    @Operation(summary = "Whole-class monthly attendance grid, one row per student keyed by day-of-month")
    public ResponseEntity<ApiResponse<List<MonthlyAttendanceRowDto>>> getMonthly(
            @RequestParam Long classId,
            @RequestParam Long sectionId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success("Monthly attendance retrieved successfully",
                studentAttendanceService.getMonthly(classId, sectionId, year, month)));
    }
}
