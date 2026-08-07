package com.school.sms.controller;

import com.school.sms.dto.request.HostelFeeRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.HostelFeeDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.service.HostelFeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hostel-fees")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Hostel Fees", description = "Manage monthly hostel fee charges")
public class HostelFeeController {

    private final HostelFeeService hostelFeeService;

    // STUDENT/PARENT are included; StudentAccessGuard scopes them to their own record.
    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','ACCOUNTANT','STUDENT','PARENT')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','ACCOUNTANT')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List hostel fee charges, paginated, filterable by student/month/year/paid status")
    public ResponseEntity<ApiResponse<PageResponse<HostelFeeDto>>> getAll(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String paidStatus,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Hostel fee records retrieved successfully",
                hostelFeeService.getAll(studentId, month, year, paidStatus, pageable)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create a monthly hostel fee charge for a student")
    public ResponseEntity<ApiResponse<HostelFeeDto>> create(@Valid @RequestBody HostelFeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Hostel fee created successfully",
                hostelFeeService.create(request)));
    }

    @PatchMapping("/{id}/mark-paid")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Mark a hostel fee charge as paid")
    public ResponseEntity<ApiResponse<HostelFeeDto>> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Hostel fee marked as paid successfully",
                hostelFeeService.markPaid(id)));
    }
}
