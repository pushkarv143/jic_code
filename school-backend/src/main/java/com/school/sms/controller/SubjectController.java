package com.school.sms.controller;

import com.school.sms.dto.request.SubjectRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.SubjectDto;
import com.school.sms.service.SubjectService;
import com.school.sms.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Subjects", description = "Manage subjects taught within a class")
public class SubjectController {

    private final SubjectService subjectService;

    /*
     * Permission-gated, matching ClassController.SUBJECT_WRITE and the
     * SUBJECT_MANAGE check the frontend applies to the same controls. See
     * SectionController.WRITE for why a hardcoded role list was the wrong shape.
     */
    private static final String WRITE_ROLES =
            "hasAuthority('" + AppConstants.PERMISSION_AUTHORITY_PREFIX + "SUBJECT_MANAGE') or "
                    + AppConstants.ADMIN_OVERRIDE;

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a subject")
    public ResponseEntity<ApiResponse<SubjectDto>> update(@PathVariable Long id,
                                                           @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subject updated successfully", subjectService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Soft-delete a subject")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subjectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
