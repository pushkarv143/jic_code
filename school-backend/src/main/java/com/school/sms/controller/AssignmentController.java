package com.school.sms.controller;

import com.school.sms.dto.request.AssignmentFormRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.AssignmentDto;
import com.school.sms.dto.response.AssignmentSubmissionDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.service.AssignmentService;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Assignments", description = "Manage homework assignments and student submissions")
public class AssignmentController {

    private final AssignmentService assignmentService;

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','CLASS_TEACHER','RECEPTIONIST','ACCOUNTANT','STUDENT','PARENT')";
    private static final String WRITE_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','CLASS_TEACHER')";
    private static final String STUDENT_ROLE = "hasRole('STUDENT')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List assignments, paginated; a STUDENT with no classId/sectionId given is auto-scoped to their own class/section")
    public ResponseEntity<ApiResponse<PageResponse<AssignmentDto>>> getAll(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long teacherId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Assignments retrieved successfully",
                assignmentService.getAll(classId, sectionId, subjectId, teacherId, pageable)));
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create an assignment (multipart: title, description, classId, sectionId, subjectId, "
            + "assignedDate, dueDate, optional teacherId, optional file attachment)")
    public ResponseEntity<ApiResponse<AssignmentDto>> create(@Valid @ModelAttribute AssignmentFormRequest request,
                                                              @RequestPart(value = "file", required = false) MultipartFile file) {
        AssignmentDto created = assignmentService.create(request, file);
        URI location = URI.create("/api/v1/assignments/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Assignment created successfully", created));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update an assignment (multipart, same shape as create; file is optional and replaces the existing one)")
    public ResponseEntity<ApiResponse<AssignmentDto>> update(@PathVariable Long id,
                                                              @Valid @ModelAttribute AssignmentFormRequest request,
                                                              @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Assignment updated successfully", assignmentService.update(id, request, file)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete an assignment")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assignmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/submissions")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "List every student's submission for an assignment (teacher/admin grading view)")
    public ResponseEntity<ApiResponse<List<AssignmentSubmissionDto>>> getSubmissions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Submissions retrieved successfully", assignmentService.getSubmissions(id)));
    }

    @GetMapping("/{assignmentId}/submissions/my")
    @PreAuthorize(STUDENT_ROLE)
    @Operation(summary = "Get the current student's own submission for an assignment (null data if not yet submitted)")
    public ResponseEntity<ApiResponse<AssignmentSubmissionDto>> getMySubmission(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(ApiResponse.success("Submission retrieved successfully", assignmentService.getMySubmission(assignmentId)));
    }

    @PostMapping(value = "/{assignmentId}/submit", consumes = "multipart/form-data")
    @PreAuthorize(STUDENT_ROLE)
    @Operation(summary = "Submit (or resubmit) a file for an assignment; auto-marked LATE if past the due date")
    public ResponseEntity<ApiResponse<AssignmentSubmissionDto>> submit(@PathVariable Long assignmentId,
                                                                        @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Assignment submitted successfully", assignmentService.submit(assignmentId, file)));
    }
}
