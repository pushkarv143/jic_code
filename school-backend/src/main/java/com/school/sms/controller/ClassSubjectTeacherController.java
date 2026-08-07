package com.school.sms.controller;

import com.school.sms.dto.request.ClassSubjectTeacherRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.ClassSubjectTeacherDto;
import com.school.sms.service.ClassSubjectTeacherService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/class-subject-teacher")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Class-Subject-Teacher Mapping", description = "Assign teachers to a section's subjects")
public class ClassSubjectTeacherController {

    private final ClassSubjectTeacherService classSubjectTeacherService;

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','CLASS_TEACHER','RECEPTIONIST','ACCOUNTANT')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List class-subject-teacher mappings, optionally filtered by section/subject/teacher")
    public ResponseEntity<ApiResponse<List<ClassSubjectTeacherDto>>> getAll(
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long teacherId) {
        return ResponseEntity.ok(ApiResponse.success("Mappings retrieved successfully",
                classSubjectTeacherService.getAll(sectionId, subjectId, teacherId)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Assign a teacher to teach a subject in a section")
    public ResponseEntity<ApiResponse<ClassSubjectTeacherDto>> create(@Valid @RequestBody ClassSubjectTeacherRequest request) {
        ClassSubjectTeacherDto created = classSubjectTeacherService.create(request);
        URI location = URI.create("/api/v1/class-subject-teacher/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Mapping created successfully", created));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Remove a class-subject-teacher mapping")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        classSubjectTeacherService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
