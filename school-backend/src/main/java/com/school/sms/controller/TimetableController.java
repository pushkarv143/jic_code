package com.school.sms.controller;

import com.school.sms.dto.request.SaveTimetableRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.TimetableSlotDto;
import com.school.sms.service.TimetableService;
import com.school.sms.util.AppConstants;
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

    /**
     * Browsing and editing any class's or any teacher's week belongs to the office
     * that builds the timetable.
     *
     * <p>This replaces an earlier decision that a timetable is "readable by everyone
     * the school teaches". The reasoning then was that a student turns up to it
     * anyway; the reasoning now is that a student turns up to <em>their own</em>, and
     * being able to enumerate any class's week — or any named teacher's whereabouts,
     * period by period — is more than that needs. The self-service paths below serve
     * the legitimate case without a browsable id.
     */
    private static final String MANAGEMENT_ONLY = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    /**
     * "My own week." Teachers, students and parents reach their timetable through
     * these and nothing else; the service resolves whose it is from the token, and
     * StudentAccessGuard holds a parent to their own children.
     *
     * <p>Management is included so one screen can serve every role rather than
     * branching, and because they may see everything regardless.
     */
    private static final String SELF_SERVICE_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','CLASS_TEACHER','STUDENT','PARENT')";

    /**
     * Assembling the week is the administrator's job.
     *
     * <p>This was {@code hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')} —
     * three roles, and unchangeable. It is now the TIMETABLE_MANAGE grant, which
     * {@code database/16_timetable_permission.sql} gives to SUPER_ADMIN alone. Two
     * things follow: the timetable is administrator-only by default, and a school
     * that wants its principal to help can grant the permission on the Roles &amp;
     * Permissions screen instead of asking for a code change.
     *
     * <p>Reads are untouched — a teacher still gets their own week from
     * {@code /timetable/me} and the class-wide views stay management-only.
     */
    private static final String WRITE_ROLES =
            "hasAuthority('" + AppConstants.PERMISSION_AUTHORITY_PREFIX + "TIMETABLE_MANAGE') or "
                    + AppConstants.ADMIN_OVERRIDE;

    @GetMapping("/classes/{classId}/sections/{sectionId}")
    @PreAuthorize(MANAGEMENT_ONLY)
    @Operation(summary = "One section's week, with a clashWarning on any slot that double-books a teacher or room")
    public ResponseEntity<ApiResponse<List<TimetableSlotDto>>> getForSection(@PathVariable Long classId,
                                                                             @PathVariable Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success("Timetable retrieved successfully",
                timetableService.getForSection(classId, sectionId)));
    }

    @GetMapping("/classes/{classId}")
    @PreAuthorize(MANAGEMENT_ONLY)
    @Operation(summary = "Every section of a class, for the class-wide grid")
    public ResponseEntity<ApiResponse<List<TimetableSlotDto>>> getForClass(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.success("Timetable retrieved successfully",
                timetableService.getForClass(classId)));
    }

    @GetMapping("/teachers/{teacherId}")
    @PreAuthorize(MANAGEMENT_ONLY)
    @Operation(summary = "One teacher's week across every section they are timetabled in")
    public ResponseEntity<ApiResponse<List<TimetableSlotDto>>> getForTeacher(@PathVariable Long teacherId) {
        return ResponseEntity.ok(ApiResponse.success("Teacher timetable retrieved successfully",
                timetableService.getForTeacher(teacherId)));
    }

    @GetMapping("/me")
    @PreAuthorize(SELF_SERVICE_ROLES)
    @Operation(summary = "The signed-in caller's own week — teaching periods for a teacher, "
            + "the enrolled class's week for a student (no id to tamper with)")
    public ResponseEntity<ApiResponse<List<TimetableSlotDto>>> getMine() {
        return ResponseEntity.ok(ApiResponse.success("Timetable retrieved successfully",
                timetableService.getForCurrentUser()));
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize(SELF_SERVICE_ROLES)
    @Operation(summary = "The week of the class a student is enrolled in; a parent is held to their own children")
    public ResponseEntity<ApiResponse<List<TimetableSlotDto>>> getForStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success("Student timetable retrieved successfully",
                timetableService.getForStudent(studentId)));
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
