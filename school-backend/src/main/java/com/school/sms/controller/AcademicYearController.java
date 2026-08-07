package com.school.sms.controller;

import com.school.sms.dto.request.AcademicYearRequest;
import com.school.sms.dto.response.AcademicYearDto;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.service.AcademicYearService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/academic-years")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Academic Years", description = "Manage the school's academic year calendar")
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','CLASS_TEACHER','RECEPTIONIST','ACCOUNTANT')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List all academic years")
    public ResponseEntity<ApiResponse<List<AcademicYearDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Academic years retrieved successfully", academicYearService.getAll()));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create a new academic year")
    public ResponseEntity<ApiResponse<AcademicYearDto>> create(@Valid @RequestBody AcademicYearRequest request) {
        AcademicYearDto created = academicYearService.create(request);
        URI location = URI.create("/api/v1/academic-years/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Academic year created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update an academic year")
    public ResponseEntity<ApiResponse<AcademicYearDto>> update(@PathVariable Long id,
                                                                @Valid @RequestBody AcademicYearRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Academic year updated successfully", academicYearService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete an academic year")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        academicYearService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/set-current")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Mark an academic year as the current one, unsetting all others")
    public ResponseEntity<ApiResponse<AcademicYearDto>> setCurrent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Current academic year updated successfully", academicYearService.setCurrent(id)));
    }
}
