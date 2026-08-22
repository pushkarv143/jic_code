package com.school.sms.controller;

import com.school.sms.dto.request.MyClassOfficialRequest;
import com.school.sms.dto.request.MyClassStudentUpdateRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.ClassOfficialDto;
import com.school.sms.dto.response.HomeroomDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudentDto;
import com.school.sms.service.MyClassService;
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

import java.util.List;

/**
 * The class teacher's own section — the roster, the student records on it, and the
 * class posts held within it.
 *
 * <p><b>No class or section id appears anywhere in this API.</b> Every route
 * resolves the section from {@code sections.class_teacher_id} for the caller, so
 * there is no parameter to tamper with and no need for the caller to know their own
 * section id. That is what separates this module from
 * {@link StudentController}, which is addressed by id and therefore has to check
 * each one against a scope.
 *
 * <p>Two independent gates apply to every route:
 * <ol>
 *   <li>the {@code MY_CLASS_*} permission — may this kind of user use the module at
 *       all, which an administrator can revoke per role at runtime;</li>
 *   <li>{@code HomeroomGuard} — and only on the section they actually hold. A caller
 *       with the permission but no homeroom assignment gets 403, which is the case
 *       for 27 of the 44 current CLASS_TEACHER role holders.</li>
 * </ol>
 *
 * <p>Attendance, marks and notices are deliberately absent: those endpoints already
 * exist and are already scoped by {@code SectionAccessGuard}, which grants a teacher
 * the sections they are homeroom of <em>or</em> teach a subject in — the right rule
 * for marking a register. The My Class screens call them directly rather than
 * keeping a second copy here. Class posts do live here, because their school-wide
 * endpoint is management-only and a homeroom-scoped path was the way to give a class
 * teacher that power without widening the admin one.
 */
@RestController
@RequestMapping("/api/v1/my-class")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "My Class", description = "The section the caller is class teacher of")
public class MyClassController {

    private final MyClassService myClassService;

    private static final String VIEW =
            "hasAuthority('" + AppConstants.PERMISSION_AUTHORITY_PREFIX + "MY_CLASS_VIEW') or "
                    + AppConstants.ADMIN_OVERRIDE;

    private static final String MANAGE_ROSTER =
            "hasAuthority('" + AppConstants.PERMISSION_AUTHORITY_PREFIX + "MY_CLASS_ROSTER_MANAGE') or "
                    + AppConstants.ADMIN_OVERRIDE;

    private static final String MANAGE_OFFICIALS =
            "hasAuthority('" + AppConstants.PERMISSION_AUTHORITY_PREFIX + "MY_CLASS_OFFICIALS_MANAGE') or "
                    + AppConstants.ADMIN_OVERRIDE;

    @GetMapping
    @PreAuthorize(VIEW)
    @Operation(summary = "The section the caller is class teacher of")
    public ResponseEntity<ApiResponse<HomeroomDto>> getMyClass() {
        return ResponseEntity.ok(ApiResponse.success("Class retrieved successfully", myClassService.getMyClass()));
    }

    /**
     * The roster. Takes search/status/paging but deliberately no classId or
     * sectionId — see the class comment.
     */
    @GetMapping("/students")
    @PreAuthorize(VIEW)
    @Operation(summary = "Students on the caller's own class roster")
    public ResponseEntity<ApiResponse<PageResponse<StudentDto>>> getMyStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "rollNumber") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        return ResponseEntity.ok(ApiResponse.success("Students retrieved successfully",
                myClassService.getMyStudents(search, status, page, size, sortBy, sortDirection)));
    }

    /*
     * There is deliberately no "add a student" route here.
     *
     * Admitting a student is the office's job. It creates a login, an admission
     * number and a guardian record, and decides which class the child joins — none
     * of which belongs to a class teacher, even for their own class. This module
     * briefly had a POST for it; it was removed because a class teacher being able
     * to enrol a pupil is not the same thing as being able to keep their records
     * up to date, and only the second was ever wanted.
     *
     * Admissions go through StudentController.create, which is gated on
     * STUDENT_CREATE — a grant no teaching role holds.
     */

    @PutMapping("/students/{studentId}")
    @PreAuthorize(MANAGE_ROSTER)
    @Operation(summary = "Update a student on the caller's own class roster")
    public ResponseEntity<ApiResponse<StudentDto>> updateStudent(
            @PathVariable Long studentId,
            @Valid @RequestBody MyClassStudentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Student updated successfully",
                myClassService.updateStudent(studentId, request)));
    }

    // ---------------------------------------------------------------------
    // Class posts — head boy, head girl, monitor and the rest.
    //
    // Reading them needs only MY_CLASS_VIEW: who holds a post is roster
    // information. Changing them is a separate grant, so an organisation can let
    // the class teacher maintain student records while reserving appointments for
    // the principal, or the other way round.
    //
    // The school-wide equivalent under /api/v1/classes/{classId}/officials stays
    // management-only and is untouched.
    // ---------------------------------------------------------------------

    @GetMapping("/officials")
    @PreAuthorize(VIEW)
    @Operation(summary = "Current holders of every post in the caller's own class")
    public ResponseEntity<ApiResponse<List<ClassOfficialDto>>> getOfficials() {
        return ResponseEntity.ok(ApiResponse.success("Class officials retrieved successfully",
                myClassService.getOfficials()));
    }

    @PostMapping("/officials")
    @PreAuthorize(MANAGE_OFFICIALS)
    @Operation(summary = "Appoint one of the caller's own students to a post in their class")
    public ResponseEntity<ApiResponse<ClassOfficialDto>> appointOfficial(
            @Valid @RequestBody MyClassOfficialRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Class official appointed successfully",
                myClassService.appointOfficial(request)));
    }

    @DeleteMapping("/officials/{officialId}")
    @PreAuthorize(MANAGE_OFFICIALS)
    @Operation(summary = "End an appointment in the caller's own class, leaving the post vacant")
    public ResponseEntity<Void> endOfficial(@PathVariable Long officialId) {
        myClassService.endOfficial(officialId);
        return ResponseEntity.noContent().build();
    }
}
