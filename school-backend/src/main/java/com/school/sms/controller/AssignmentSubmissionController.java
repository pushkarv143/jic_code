package com.school.sms.controller;

import com.school.sms.dto.request.GradeSubmissionRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.AssignmentSubmissionDto;
import com.school.sms.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assignment-submissions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Assignment Submissions", description = "Grade a student's assignment submission")
public class AssignmentSubmissionController {

    private final AssignmentService assignmentService;

    private static final String WRITE_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER')";

    @PatchMapping("/{id}/grade")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Grade a student's assignment submission")
    public ResponseEntity<ApiResponse<AssignmentSubmissionDto>> grade(@PathVariable Long id,
                                                                       @Valid @RequestBody GradeSubmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Submission graded successfully", assignmentService.gradeSubmission(id, request)));
    }
}
