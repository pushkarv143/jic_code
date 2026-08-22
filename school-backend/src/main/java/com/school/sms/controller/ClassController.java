package com.school.sms.controller;

import com.school.sms.dto.request.SchoolClassRequest;
import com.school.sms.dto.request.SectionRequest;
import com.school.sms.dto.request.SubjectRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.ClassOverviewDto;
import com.school.sms.dto.response.ClassTeacherAvailabilityDto;
import com.school.sms.dto.response.SchoolClassDto;
import com.school.sms.dto.response.TeacherWorkloadDto;
import com.school.sms.dto.response.SectionDto;
import com.school.sms.dto.response.SubjectDto;
import com.school.sms.service.ClassOverviewService;
import com.school.sms.service.SchoolClassService;
import com.school.sms.service.SectionService;
import com.school.sms.service.SubjectService;
import com.school.sms.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
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
    private final ClassOverviewService classOverviewService;

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','RECEPTIONIST','ACCOUNTANT')";
    /*
     * Permission-gated rather than role-gated, for the reason spelled out on
     * SectionController.WRITE: a hardcoded role list cannot be reconfigured, so
     * revoking the grant would hide the button and still accept the request.
     *
     * Split by what each endpoint actually writes, matching the grants the
     * frontend checks on the same controls:
     *
     *   CLASS_MANAGE   - the class itself, and its nested section create
     *   SUBJECT_MANAGE - the class's subjects
     *
     * No behaviour change on a seeded database: SUPER_ADMIN, PRINCIPAL and
     * VICE_PRINCIPAL all hold both today.
     */
    private static final String WRITE_ROLES =
            "hasAuthority('" + AppConstants.PERMISSION_AUTHORITY_PREFIX + "CLASS_MANAGE') or "
                    + AppConstants.ADMIN_OVERRIDE;

    private static final String SUBJECT_WRITE =
            "hasAuthority('" + AppConstants.PERMISSION_AUTHORITY_PREFIX + "SUBJECT_MANAGE') or "
                    + AppConstants.ADMIN_OVERRIDE;

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

    @GetMapping("/{id}/overview")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Strength, capacity, gender split, class/subject teachers, officials, cross-module "
            + "stats and setup warnings for one class, in one response")
    public ResponseEntity<ApiResponse<ClassOverviewDto>> getOverview(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success("Class overview retrieved successfully",
                classOverviewService.getOverview(id, startDate, endDate)));
    }

    @GetMapping("/class-teacher-availability")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Teachers who already have a homeroom and where, so the assignment UI can grey them out "
            + "(a teacher may be class teacher of only one section)")
    public ResponseEntity<ApiResponse<List<ClassTeacherAvailabilityDto>>> getClassTeacherAvailability() {
        return ResponseEntity.ok(ApiResponse.success("Class teacher availability retrieved successfully",
                classOverviewService.getClassTeacherAvailability()));
    }

    @GetMapping("/teacher-workload")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "School-wide teacher workload — mappings, sections and weekly periods, heaviest first")
    public ResponseEntity<ApiResponse<List<TeacherWorkloadDto>>> getTeacherWorkload() {
        return ResponseEntity.ok(ApiResponse.success("Teacher workload retrieved successfully",
                classOverviewService.getTeacherWorkload()));
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
    @PreAuthorize(SUBJECT_WRITE)
    @Operation(summary = "Create a new subject under a class")
    public ResponseEntity<ApiResponse<SubjectDto>> createSubject(@PathVariable Long classId,
                                                                  @Valid @RequestBody SubjectRequest request) {
        SubjectDto created = subjectService.create(classId, request);
        URI location = URI.create("/api/v1/subjects/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Subject created successfully", created));
    }
}
