package com.school.sms.controller;

import com.school.sms.dto.request.ExamTypeRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.ExamTypeDto;
import com.school.sms.service.ExamTypeService;
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
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/exam-types")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Exam Types", description = "Manage exam types (Unit Test, Half Yearly, Final...)")
public class ExamTypeController {

    private final ExamTypeService examTypeService;

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','CLASS_TEACHER','RECEPTIONIST','ACCOUNTANT','STUDENT','PARENT')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List all exam types")
    public ResponseEntity<ApiResponse<List<ExamTypeDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Exam types retrieved successfully", examTypeService.getAll()));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create a new exam type")
    public ResponseEntity<ApiResponse<ExamTypeDto>> create(@Valid @RequestBody ExamTypeRequest request) {
        ExamTypeDto created = examTypeService.create(request);
        URI location = URI.create("/api/v1/exam-types/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Exam type created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update an exam type")
    public ResponseEntity<ApiResponse<ExamTypeDto>> update(@PathVariable Long id,
                                                            @Valid @RequestBody ExamTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Exam type updated successfully", examTypeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete an exam type")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        examTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
