package com.school.sms.controller;

import com.school.sms.dto.request.LeaveApplicationRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.LeaveApplicationDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.service.LeaveApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leave-applications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Leave Applications", description = "Apply for and approve/reject staff and student leave")
public class LeaveApplicationController {

    private final LeaveApplicationService leaveApplicationService;

    // Class teachers are included here so they can approve/reject student leave;
    // the service layer restricts them to STUDENT-type applications only.
    private static final String ADMIN_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','CLASS_TEACHER')";

    @GetMapping
    @PreAuthorize(ADMIN_ROLES)
    @Operation(summary = "Admin/management view of every leave application, filterable by applicant type/status")
    public ResponseEntity<ApiResponse<PageResponse<LeaveApplicationDto>>> getAll(
            @RequestParam(required = false) String applicantType,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Leave applications retrieved successfully",
                leaveApplicationService.getAll(applicantType, status, pageable)));
    }

    @GetMapping("/my")
    @Operation(summary = "The current user's own leave applications")
    public ResponseEntity<ApiResponse<PageResponse<LeaveApplicationDto>>> getMy(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Your leave applications retrieved successfully",
                leaveApplicationService.getMy(pageable)));
    }

    @PostMapping
    @Operation(summary = "Apply for leave (applicant is always the current user)")
    public ResponseEntity<ApiResponse<LeaveApplicationDto>> create(@Valid @RequestBody LeaveApplicationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Leave application submitted successfully",
                leaveApplicationService.create(request)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize(ADMIN_ROLES)
    @Operation(summary = "Approve a leave application")
    public ResponseEntity<ApiResponse<LeaveApplicationDto>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Leave application approved successfully",
                leaveApplicationService.approve(id)));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize(ADMIN_ROLES)
    @Operation(summary = "Reject a leave application")
    public ResponseEntity<ApiResponse<LeaveApplicationDto>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Leave application rejected successfully",
                leaveApplicationService.reject(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Withdraw a leave application (only its own applicant, only while still pending)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        leaveApplicationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
