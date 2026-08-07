package com.school.sms.controller;

import com.school.sms.dto.request.ExamScheduleRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.ExamScheduleDto;
import com.school.sms.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exam-schedules")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Exam Schedules", description = "Update/delete individual exam subject schedules")
public class ExamScheduleController {

    private final ExamService examService;

    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update an exam schedule")
    public ResponseEntity<ApiResponse<ExamScheduleDto>> update(@PathVariable Long id,
                                                                @Valid @RequestBody ExamScheduleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Exam schedule updated successfully", examService.updateSchedule(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete an exam schedule (cascades to its marks)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        examService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
