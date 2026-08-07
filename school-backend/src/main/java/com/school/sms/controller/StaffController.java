package com.school.sms.controller;

import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StaffDto;
import com.school.sms.service.StaffService;
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
 * Minimal read-only staff directory — not full CRUD (non-teaching staff
 * onboarding isn't in scope), just enough for Payroll's employee search to
 * find a STAFF-type employee the same way it already finds teachers.
 */
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Staff", description = "Read-only staff directory, used by Payroll's employee search")
public class StaffController {

    private final StaffService staffService;

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','ACCOUNTANT')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Search active staff by name/employee id, paginated")
    public ResponseEntity<ApiResponse<PageResponse<StaffDto>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        return ResponseEntity.ok(ApiResponse.success("Staff retrieved successfully",
                staffService.search(search, page, size, sortBy, sortDirection)));
    }
}
