package com.school.sms.controller;

import com.school.sms.dto.request.HostelStudentRequest;
import com.school.sms.dto.request.HostelStudentVacateRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.HostelStudentDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.service.HostelStudentService;
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
@RequestMapping("/api/v1/hostel-students")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Hostel Students", description = "Allocate/vacate students to hostel rooms")
public class HostelStudentController {

    private final HostelStudentService hostelStudentService;

    // STUDENT/PARENT are included; StudentAccessGuard scopes them to their own record.
    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','STUDENT','PARENT')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List hostel student allocations, paginated, filterable by student/room/status")
    public ResponseEntity<ApiResponse<PageResponse<HostelStudentDto>>> getAll(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Hostel student records retrieved successfully",
                hostelStudentService.getAll(studentId, roomId, status, pageable)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Allocate a student to a hostel room")
    public ResponseEntity<ApiResponse<HostelStudentDto>> allocate(@Valid @RequestBody HostelStudentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Student allocated to hostel room successfully",
                hostelStudentService.allocate(request)));
    }

    @PatchMapping("/{id}/vacate")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Mark a student's hostel allocation as vacated")
    public ResponseEntity<ApiResponse<HostelStudentDto>> vacate(@PathVariable Long id,
                                                                 @RequestBody(required = false) HostelStudentVacateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Student marked as vacated successfully",
                hostelStudentService.vacate(id, request != null ? request : new HostelStudentVacateRequest())));
    }
}
