package com.school.sms.controller;

import com.school.sms.dto.request.DesignationRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.DesignationDto;
import com.school.sms.service.DesignationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/designations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Designations", description = "Manage teacher/staff designations")
public class DesignationController {

    private final DesignationService designationService;

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','RECEPTIONIST','ACCOUNTANT')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List all designations")
    public ResponseEntity<ApiResponse<List<DesignationDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Designations retrieved successfully", designationService.getAll()));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create a new designation")
    public ResponseEntity<ApiResponse<DesignationDto>> create(@Valid @RequestBody DesignationRequest request) {
        DesignationDto created = designationService.create(request);
        URI location = URI.create("/api/v1/designations/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Designation created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a designation")
    public ResponseEntity<ApiResponse<DesignationDto>> update(@PathVariable Long id,
                                                               @Valid @RequestBody DesignationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Designation updated successfully", designationService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete a designation")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        designationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
