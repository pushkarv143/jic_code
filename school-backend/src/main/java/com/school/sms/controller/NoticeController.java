package com.school.sms.controller;

import com.school.sms.dto.request.NoticeFormRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.NoticeDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.service.NoticeService;
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
import org.springframework.web.bind.annotation.ModelAttribute;
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
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notices", description = "School notice board, scoped by target role and expiry")
public class NoticeController {

    private final NoticeService noticeService;

    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List notices scoped to the caller's role and non-expired ones; "
            + "admin roles may pass includeExpired=true or targetRole= to see everything")
    public ResponseEntity<ApiResponse<PageResponse<NoticeDto>>> getAll(
            @RequestParam(required = false, defaultValue = "false") boolean includeExpired,
            @RequestParam(required = false) String targetRole,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Notices retrieved successfully",
                noticeService.getAll(includeExpired, targetRole, pageable)));
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Publish a notice (multipart: title, description, optional targetRole, optional expiryDate, optional file)")
    public ResponseEntity<ApiResponse<NoticeDto>> create(@Valid @ModelAttribute NoticeFormRequest request,
                                                          @RequestPart(value = "file", required = false) MultipartFile file) {
        NoticeDto created = noticeService.create(request, file);
        URI location = URI.create("/api/v1/notices/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Notice created successfully", created));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a notice (multipart, same shape as create)")
    public ResponseEntity<ApiResponse<NoticeDto>> update(@PathVariable Long id,
                                                          @Valid @ModelAttribute NoticeFormRequest request,
                                                          @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Notice updated successfully", noticeService.update(id, request, file)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete a notice")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
