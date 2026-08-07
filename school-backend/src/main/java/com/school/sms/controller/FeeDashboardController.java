package com.school.sms.controller;

import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.FeesDuesSummaryDto;
import com.school.sms.service.StudentFeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Small, separate controller for the /api/v1/fees/** dashboard-aggregate
 * routes — kept apart from StudentFeeController (/api/v1/student-fees) since
 * the URL prefix per the module contract is "fees", not "student-fees".
 */
@RestController
@RequestMapping("/api/v1/fees")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fee Dashboard", description = "Aggregate fee figures for dashboard cards")
public class FeeDashboardController {

    private final StudentFeeService studentFeeService;

    private static final String READ_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','ACCOUNTANT')";

    @GetMapping("/dues-summary")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Aggregate total due/collected/outstanding and student count, optionally filtered by class/academic year")
    public ResponseEntity<ApiResponse<FeesDuesSummaryDto>> getDuesSummary(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Dues summary retrieved successfully",
                studentFeeService.getDuesSummary(classId, academicYearId)));
    }
}
