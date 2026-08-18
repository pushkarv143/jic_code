package com.school.sms.controller;

import com.school.sms.dto.request.SaveTimetableRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.TimetableSlotDto;
import com.school.sms.service.TimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/timetable")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Timetable", description = "Weekly period grid per section, with teacher and room clash detection")
public class TimetableController {

    private final TimetableService timetableService;

    // A timetable is what a student turns up to, so it is readable by everyone
    // the school teaches, not just staff.
    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','CLASS_TEACHER','RECEPTIONIST',"
                    + "'ACCOUNTANT','STUDENT','PARENT')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @GetMapping("/classes/{classId}/sections/{sectionId}")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "One section's week, with a clashWarning on any slot that double-books a teacher or room")
    public ResponseEntity<ApiResponse<List<TimetableSlotDto>>> getForSection(@PathVariable Long classId,
                                                                             @PathVariable Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success("Timetable retrieved successfully",
                timetableService.getForSection(classId, sectionId)));
    }

    @GetMapping("/classes/{classId}")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Every section of a class, for the class-wide grid")
    public ResponseEntity<ApiResponse<List<TimetableSlotDto>>> getForClass(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.success("Timetable retrieved successfully",
                timetableService.getForClass(classId)));
    }

    @GetMapping("/teachers/{teacherId}")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "One teacher's week across every section they are timetabled in")
    public ResponseEntity<ApiResponse<List<TimetableSlotDto>>> getForTeacher(@PathVariable Long teacherId) {
        return ResponseEntity.ok(ApiResponse.success("Teacher timetable retrieved successfully",
                timetableService.getForTeacher(teacherId)));
    }

    @PutMapping("/classes/{classId}/sections/{sectionId}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Replace a section's whole week in one call (an empty slot list clears it)")
    public ResponseEntity<ApiResponse<List<TimetableSlotDto>>> saveForSection(
            @PathVariable Long classId,
            @PathVariable Long sectionId,
            @Valid @RequestBody SaveTimetableRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Timetable saved successfully",
                timetableService.saveForSection(classId, sectionId, request)));
    }
}
