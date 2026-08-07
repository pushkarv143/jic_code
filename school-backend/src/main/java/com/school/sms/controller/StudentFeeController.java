package com.school.sms.controller;

import com.school.sms.dto.request.GenerateStudentFeesRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.GenerateFeesResultDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudentFeeDto;
import com.school.sms.service.StudentFeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student-fees")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Student Fees", description = "Per-student fee ledger, generated from fee structures")
public class StudentFeeController {

    private final StudentFeeService studentFeeService;

    // STUDENT/PARENT are included; StudentAccessGuard scopes them to their own record.
    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','ACCOUNTANT','STUDENT','PARENT')";
    private static final String COLLECT_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','ACCOUNTANT')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List student fee records, paginated, filterable by student/class/section/status/academic year")
    public ResponseEntity<ApiResponse<PageResponse<StudentFeeDto>>> getAll(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long academicYearId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Student fees retrieved successfully",
                studentFeeService.getAll(studentId, classId, sectionId, status, academicYearId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Get a single student fee record including its payment history")
    public ResponseEntity<ApiResponse<StudentFeeDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Student fee retrieved successfully", studentFeeService.getById(id)));
    }

    @PostMapping("/generate")
    @PreAuthorize(COLLECT_ROLES)
    @Operation(summary = "Generate student_fees rows for every active student in a class against the given fee structures")
    public ResponseEntity<ApiResponse<GenerateFeesResultDto>> generate(@Valid @RequestBody GenerateStudentFeesRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Student fees generated successfully", studentFeeService.generate(request)));
    }
}
