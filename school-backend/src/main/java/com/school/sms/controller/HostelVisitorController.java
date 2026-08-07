package com.school.sms.controller;

import com.school.sms.dto.request.HostelVisitorRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.HostelVisitorDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.service.HostelVisitorService;
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
@RequestMapping("/api/v1/hostel-visitors")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Hostel Visitors", description = "Check hostel visitors in/out")
public class HostelVisitorController {

    private final HostelVisitorService hostelVisitorService;

    // STUDENT/PARENT are included; StudentAccessGuard scopes them to their own record.
    // SECURITY_GUARD is the front-desk role that actually checks visitors in/out.
    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','SECURITY_GUARD','STUDENT','PARENT')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','SECURITY_GUARD')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List hostel visitor records, paginated, filterable by student")
    public ResponseEntity<ApiResponse<PageResponse<HostelVisitorDto>>> getAll(
            @RequestParam(required = false) Long studentId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Hostel visitor records retrieved successfully",
                hostelVisitorService.getAll(studentId, pageable)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Check in a hostel visitor")
    public ResponseEntity<ApiResponse<HostelVisitorDto>> checkIn(@Valid @RequestBody HostelVisitorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Visitor checked in successfully",
                hostelVisitorService.checkIn(request)));
    }

    @PatchMapping("/{id}/checkout")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Check out a hostel visitor")
    public ResponseEntity<ApiResponse<HostelVisitorDto>> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Visitor checked out successfully",
                hostelVisitorService.checkOut(id)));
    }
}
