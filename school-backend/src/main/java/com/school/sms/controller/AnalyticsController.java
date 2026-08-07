package com.school.sms.controller;

import com.school.sms.dto.response.AnalyticsDashboardDto;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Analytics", description = "Combined management analytics landing page")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','ACCOUNTANT')")
    @Operation(summary = "Headline student/teacher/fee/attendance figures composed from the /reports endpoints")
    public ResponseEntity<ApiResponse<AnalyticsDashboardDto>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Analytics dashboard retrieved successfully",
                analyticsService.getDashboard()));
    }
}
