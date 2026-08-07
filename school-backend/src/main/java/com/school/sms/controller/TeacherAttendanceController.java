package com.school.sms.controller;

import com.school.sms.dto.request.MarkTeacherAttendanceRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.TeacherAttendanceRecordDto;
import com.school.sms.dto.response.TeacherAttendanceRowDto;
import com.school.sms.service.TeacherAttendanceService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/attendance/teachers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Teacher Attendance", description = "Mark and report on daily teacher attendance")
public class TeacherAttendanceController {

    private final TeacherAttendanceService teacherAttendanceService;

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','CLASS_TEACHER','RECEPTIONIST','ACCOUNTANT')";
    private static final String MARK_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Get the attendance-marking grid for all active teachers on a date")
    public ResponseEntity<ApiResponse<List<TeacherAttendanceRowDto>>> getGrid(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success("Attendance grid retrieved successfully",
                teacherAttendanceService.getGrid(date)));
    }

    @PostMapping("/mark")
    @PreAuthorize(MARK_ROLES)
    @Operation(summary = "Mark (upsert) teacher attendance for a date in one batch")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> mark(@Valid @RequestBody MarkTeacherAttendanceRequest request) {
        int count = teacherAttendanceService.mark(request);
        return ResponseEntity.ok(ApiResponse.success(count + " teacher(s) marked successfully", Map.of("marked", count)));
    }

    @GetMapping("/report")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Paginated teacher attendance records, filterable by teacher/date range")
    public ResponseEntity<ApiResponse<PageResponse<TeacherAttendanceRecordDto>>> getReport(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Attendance report retrieved successfully",
                teacherAttendanceService.getReport(teacherId, startDate, endDate, pageable)));
    }
}
