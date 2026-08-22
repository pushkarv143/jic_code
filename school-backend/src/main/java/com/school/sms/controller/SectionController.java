package com.school.sms.controller;

import com.school.sms.dto.request.AssignClassTeacherRequest;
import com.school.sms.dto.request.SectionRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.SectionDto;
import com.school.sms.service.SectionService;
import com.school.sms.util.AppConstants;
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

    /**
     * Gated on the SECTION_MANAGE grant rather than a hardcoded role list.
     *
     * <p>This was {@code hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')},
     * which made the policy unchangeable: revoking SECTION_MANAGE from
     * VICE_PRINCIPAL on the Roles &amp; Permissions screen hid the controls in the
     * UI while the API went on accepting their requests. Reading the permission
     * makes the two agree, and makes "only the administrator may appoint a class
     * teacher" a setting an organisation can apply — revoke SECTION_MANAGE from
     * PRINCIPAL and VICE_PRINCIPAL and the ADMIN_OVERRIDE below is all that is
     * left.
     *
     * <p>No behaviour change on a seeded database: all three roles hold
     * SECTION_MANAGE today, so the same callers are admitted as before.
     */
    private static final String WRITE =
            "hasAuthority('" + AppConstants.PERMISSION_AUTHORITY_PREFIX + "SECTION_MANAGE') or "
                    + AppConstants.ADMIN_OVERRIDE;

    @PutMapping("/{id}")
    @PreAuthorize(WRITE)
    @Operation(summary = "Update a section")
    public ResponseEntity<ApiResponse<SectionDto>> update(@PathVariable Long id,
                                                           @Valid @RequestBody SectionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Section updated successfully", sectionService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE)
    @Operation(summary = "Delete a section")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sectionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/assign-class-teacher")
    @PreAuthorize(WRITE)
    @Operation(summary = "Assign a class teacher to a section (promotes a plain TEACHER to CLASS_TEACHER)")
    public ResponseEntity<ApiResponse<SectionDto>> assignClassTeacher(@PathVariable Long id,
                                                                       @Valid @RequestBody AssignClassTeacherRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Class teacher assigned successfully",
                sectionService.assignClassTeacher(id, request)));
    }
}
