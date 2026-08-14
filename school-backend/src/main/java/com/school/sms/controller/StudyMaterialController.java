package com.school.sms.controller;

import com.school.sms.dto.request.StudyMaterialRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudyMaterialDto;
import com.school.sms.service.StudyMaterialService;
import com.school.sms.util.AppConstants;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/study-materials")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Study Materials", description = "Share and browse teaching resources for a class or section")
public class StudyMaterialController {

    private final StudyMaterialService studyMaterialService;

    // Who may reach the library at all. Which materials they actually get back is
    // decided in the service: a teacher sees the classes they teach (drafts
    // included), a student only published materials addressed to their own class
    // and section. Role gate here, row filter there.
    private static final String READ_ROLES =
            "hasAuthority('PERM_MATERIAL_VIEW') or " + AppConstants.ADMIN_OVERRIDE;
    // Uploading is permission-gated rather than role-gated, so revoking
    // MATERIAL_MANAGE from a role takes effect without a code change. Which section
    // they may upload to is still decided per request by SectionAccessGuard.
    // ADMIN_OVERRIDE keeps a missing grant row from locking the administrator out.
    private static final String WRITE_ROLES =
            "hasAuthority('PERM_MATERIAL_MANAGE') or " + AppConstants.ADMIN_OVERRIDE;

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List study materials visible to the caller, paginated and filterable")
    public ResponseEntity<ApiResponse<PageResponse<StudyMaterialDto>>> getAll(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String materialType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        return ResponseEntity.ok(ApiResponse.success("Study materials retrieved successfully",
                studyMaterialService.getAll(classId, sectionId, subjectId, materialType, search,
                        page, size, sortBy, sortDirection)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Get a single study material")
    public ResponseEntity<ApiResponse<StudyMaterialDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Study material retrieved successfully",
                studyMaterialService.getById(id)));
    }

    /**
     * Resolves a material's download source after re-running the visibility check.
     * Returns the URL rather than streaming the bytes, matching how photos and
     * student documents are already served from {@code /uploads}.
     */
    @GetMapping("/{id}/download")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Resolve a study material's download URL, re-checking visibility first")
    public ResponseEntity<ApiResponse<StudyMaterialDto>> getForDownload(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Download link resolved",
                studyMaterialService.getForDownload(id)));
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Share a new study material (upload a file or link to an external resource)")
    public ResponseEntity<ApiResponse<StudyMaterialDto>> create(
            @Valid @RequestPart("material") StudyMaterialRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        StudyMaterialDto created = studyMaterialService.create(request, file);
        URI location = URI.create("/api/v1/study-materials/" + created.getId());
        return ResponseEntity.created(location)
                .body(ApiResponse.success("Study material shared successfully", created));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a study material; only its uploader or management may do so")
    public ResponseEntity<ApiResponse<StudyMaterialDto>> update(
            @PathVariable Long id,
            @Valid @RequestPart("material") StudyMaterialRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Study material updated successfully",
                studyMaterialService.update(id, request, file)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Soft-delete a study material; only its uploader or management may do so")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studyMaterialService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
