package com.school.sms.controller;

import com.school.sms.dto.request.StudentTransportRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudentTransportDto;
import com.school.sms.service.StudentTransportService;
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

import java.net.URI;

@RestController
@RequestMapping("/api/v1/student-transport")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Student Transport", description = "Assign students to transport routes/pickup points")
public class StudentTransportController {

    private final StudentTransportService studentTransportService;

    // STUDENT/PARENT are included; StudentAccessGuard scopes them to their own record.
    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','RECEPTIONIST','STUDENT','PARENT')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','RECEPTIONIST')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List student transport assignments, paginated, filterable by student/route")
    public ResponseEntity<ApiResponse<PageResponse<StudentTransportDto>>> getAll(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long routeId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Student transport records retrieved successfully",
                studentTransportService.getAll(studentId, routeId, pageable)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Assign a student to a route and pickup point")
    public ResponseEntity<ApiResponse<StudentTransportDto>> create(@Valid @RequestBody StudentTransportRequest request) {
        StudentTransportDto created = studentTransportService.create(request);
        URI location = URI.create("/api/v1/student-transport/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Student assigned to transport successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a student's transport assignment")
    public ResponseEntity<ApiResponse<StudentTransportDto>> update(@PathVariable Long id,
                                                                    @Valid @RequestBody StudentTransportRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Student transport assignment updated successfully",
                studentTransportService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Remove a student's transport assignment")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentTransportService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
