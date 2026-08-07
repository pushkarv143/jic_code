package com.school.sms.controller;

import com.school.sms.dto.request.ScholarshipRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.ScholarshipDto;
import com.school.sms.service.ScholarshipService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scholarships")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Scholarships", description = "Manage student scholarships/fee waivers")
public class ScholarshipController {

    private final ScholarshipService scholarshipService;

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','ACCOUNTANT','TEACHER','CLASS_TEACHER')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','ACCOUNTANT')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List scholarships, paginated, optionally filtered by student/academic year")
    public ResponseEntity<ApiResponse<PageResponse<ScholarshipDto>>> getAll(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long academicYearId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Scholarships retrieved successfully",
                scholarshipService.getAll(studentId, academicYearId, pageable)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Award a scholarship to a student (approver is always the current user)")
    public ResponseEntity<ApiResponse<ScholarshipDto>> create(@Valid @RequestBody ScholarshipRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Scholarship created successfully", scholarshipService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a scholarship")
    public ResponseEntity<ApiResponse<ScholarshipDto>> update(@PathVariable Long id,
                                                               @Valid @RequestBody ScholarshipRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Scholarship updated successfully", scholarshipService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete a scholarship")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scholarshipService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
