package com.school.sms.controller;

import com.school.sms.dto.request.ExamRequest;
import com.school.sms.dto.request.ExamScheduleRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.ExamDto;
import com.school.sms.dto.response.ExamResultRowDto;
import com.school.sms.dto.response.ExamScheduleDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.service.ExamService;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Exams", description = "Manage exams, exam schedules and class-wide results")
public class ExamController {

    private final ExamService examService;

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','RECEPTIONIST','ACCOUNTANT','STUDENT','PARENT')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";
    private static final String STAFF_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List exams, paginated, filterable by class/academic year/exam type")
    public ResponseEntity<ApiResponse<PageResponse<ExamDto>>> getAll(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long examTypeId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Exams retrieved successfully",
                examService.getAll(classId, academicYearId, examTypeId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Get an exam by id")
    public ResponseEntity<ApiResponse<ExamDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Exam retrieved successfully", examService.getById(id)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create a new exam")
    public ResponseEntity<ApiResponse<ExamDto>> create(@Valid @RequestBody ExamRequest request) {
        ExamDto created = examService.create(request);
        URI location = URI.create("/api/v1/exams/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Exam created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update an exam")
    public ResponseEntity<ApiResponse<ExamDto>> update(@PathVariable Long id, @Valid @RequestBody ExamRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Exam updated successfully", examService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete an exam (cascades to its schedules and marks)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        examService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{examId}/schedules")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List an exam's subject schedules")
    public ResponseEntity<ApiResponse<List<ExamScheduleDto>>> getSchedules(@PathVariable Long examId) {
        return ResponseEntity.ok(ApiResponse.success("Exam schedules retrieved successfully", examService.getSchedules(examId)));
    }

    @PostMapping("/{examId}/schedules")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Add a subject schedule to an exam")
    public ResponseEntity<ApiResponse<ExamScheduleDto>> addSchedule(@PathVariable Long examId,
                                                                     @Valid @RequestBody ExamScheduleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Exam schedule created successfully", examService.addSchedule(examId, request)));
    }

    @GetMapping("/{examId}/results")
    @PreAuthorize(STAFF_ROLES)
    @Operation(summary = "Class-wide results for an exam: per-student totals, percentage and dense rank")
    public ResponseEntity<ApiResponse<List<ExamResultRowDto>>> getResults(
            @PathVariable Long examId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success("Exam results retrieved successfully",
                examService.getResults(examId, classId, sectionId)));
    }
}
