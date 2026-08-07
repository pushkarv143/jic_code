package com.school.sms.controller;

import com.school.sms.dto.request.SchoolClassRequest;
import com.school.sms.dto.request.SectionRequest;
import com.school.sms.dto.request.SubjectRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.SchoolClassDto;
import com.school.sms.dto.response.SectionDto;
import com.school.sms.dto.response.SubjectDto;
import com.school.sms.service.SchoolClassService;
import com.school.sms.service.SectionService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Classes", description = "Manage classes and their nested sections/subjects")
public class ClassController {

    private final SchoolClassService schoolClassService;
    private final SectionService sectionService;
    private final SubjectService subjectService;

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','CLASS_TEACHER','RECEPTIONIST','ACCOUNTANT')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List classes, paginated, optionally filtered by academic year")
    public ResponseEntity<ApiResponse<PageResponse<SchoolClassDto>>> getAll(
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        return ResponseEntity.ok(ApiResponse.success("Classes retrieved successfully",
                schoolClassService.getAll(academicYearId, page, size, sortBy, sortDirection)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Get a single class by id")
    public ResponseEntity<ApiResponse<SchoolClassDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Class retrieved successfully", schoolClassService.getById(id)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create a new class")
    public ResponseEntity<ApiResponse<SchoolClassDto>> create(@Valid @RequestBody SchoolClassRequest request) {
        SchoolClassDto created = schoolClassService.create(request);
        URI location = URI.create("/api/v1/classes/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Class created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a class")
    public ResponseEntity<ApiResponse<SchoolClassDto>> update(@PathVariable Long id,
                                                               @Valid @RequestBody SchoolClassRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Class updated successfully", schoolClassService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Soft-delete a class")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        schoolClassService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{classId}/sections")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List sections belonging to a class")
    public ResponseEntity<ApiResponse<List<SectionDto>>> getSections(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.success("Sections retrieved successfully", sectionService.getByClassId(classId)));
    }

    @PostMapping("/{classId}/sections")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create a new section under a class")
    public ResponseEntity<ApiResponse<SectionDto>> createSection(@PathVariable Long classId,
                                                                  @Valid @RequestBody SectionRequest request) {
        SectionDto created = sectionService.create(classId, request);
        URI location = URI.create("/api/v1/sections/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Section created successfully", created));
    }

    @GetMapping("/{classId}/subjects")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List subjects belonging to a class")
    public ResponseEntity<ApiResponse<List<SubjectDto>>> getSubjects(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.success("Subjects retrieved successfully", subjectService.getByClassId(classId)));
    }

    @PostMapping("/{classId}/subjects")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create a new subject under a class")
    public ResponseEntity<ApiResponse<SubjectDto>> createSubject(@PathVariable Long classId,
                                                                  @Valid @RequestBody SubjectRequest request) {
        SubjectDto created = subjectService.create(classId, request);
        URI location = URI.create("/api/v1/subjects/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Subject created successfully", created));
    }
}
