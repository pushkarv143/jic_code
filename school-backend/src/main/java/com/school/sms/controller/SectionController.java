package com.school.sms.controller;

import com.school.sms.dto.request.AssignClassTeacherRequest;
import com.school.sms.dto.request.SectionRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.SectionDto;
import com.school.sms.service.SectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sections")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Sections", description = "Manage class sections")
public class SectionController {

    private final SectionService sectionService;

    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";
    private static final String ASSIGN_TEACHER_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a section")
    public ResponseEntity<ApiResponse<SectionDto>> update(@PathVariable Long id,
                                                           @Valid @RequestBody SectionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Section updated successfully", sectionService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete a section")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sectionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/assign-class-teacher")
    @PreAuthorize(ASSIGN_TEACHER_ROLES)
    @Operation(summary = "Assign a class teacher to a section (promotes a plain TEACHER to CLASS_TEACHER)")
    public ResponseEntity<ApiResponse<SectionDto>> assignClassTeacher(@PathVariable Long id,
                                                                       @Valid @RequestBody AssignClassTeacherRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Class teacher assigned successfully",
                sectionService.assignClassTeacher(id, request)));
    }
}
