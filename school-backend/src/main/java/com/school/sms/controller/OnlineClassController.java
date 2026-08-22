package com.school.sms.controller;

import com.school.sms.dto.request.OnlineClassRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.OnlineClassDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.service.OnlineClassService;
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
@RequestMapping("/api/v1/online-classes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Online Classes", description = "Schedule and manage live/online class sessions")
public class OnlineClassController {

    private final OnlineClassService onlineClassService;

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','RECEPTIONIST','ACCOUNTANT','STUDENT','PARENT')";
    // Not spelled out explicitly in the round brief's Security section (only
    // assignment creation is) — mirrored from it for consistency; see the
    // round report's deviations note.
    private static final String WRITE_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List online classes, paginated; upcoming=true filters to scheduledAt >= now; "
            + "a STUDENT with no classId/sectionId given is auto-scoped to their own class/section")
    public ResponseEntity<ApiResponse<PageResponse<OnlineClassDto>>> getAll(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Boolean upcoming,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Online classes retrieved successfully",
                onlineClassService.getAll(classId, sectionId, subjectId, teacherId, upcoming, pageable)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Schedule a new online class")
    public ResponseEntity<ApiResponse<OnlineClassDto>> create(@Valid @RequestBody OnlineClassRequest request) {
        OnlineClassDto created = onlineClassService.create(request);
        URI location = URI.create("/api/v1/online-classes/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Online class created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update an online class")
    public ResponseEntity<ApiResponse<OnlineClassDto>> update(@PathVariable Long id,
                                                               @Valid @RequestBody OnlineClassRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Online class updated successfully", onlineClassService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete an online class")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        onlineClassService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
