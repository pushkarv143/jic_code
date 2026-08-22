package com.school.sms.controller;

import com.school.sms.dto.request.TeacherCreateRequest;
import com.school.sms.dto.request.TeacherSelfUpdateRequest;
import com.school.sms.dto.request.TeacherStatusRequest;
import com.school.sms.dto.request.TeacherUpdateRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.TeacherAssignmentDto;
import com.school.sms.dto.response.TeacherDto;
import com.school.sms.service.ExcelService;
import com.school.sms.service.PdfService;
import com.school.sms.service.TeacherService;
import com.school.sms.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Teachers", description = "Manage teacher profiles and their subject/section assignments")
public class TeacherController {

    private final TeacherService teacherService;
    private final PdfService pdfService;
    private final ExcelService excelService;

    // Who may read the staff directory at all. Unlike students, the rows are not
    // narrowed — colleagues legitimately need to find each other — but TeacherAccessGuard
    // blanks the sensitive half of each record (salary, DOB, address, emergency
    // contact, blood group) for anyone who is not management, the accountant, or the
    // teacher themselves. Role gate here, field gate there.
    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','RECEPTIONIST','ACCOUNTANT')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";
    private static final String SELF_SERVICE_ROLES = "hasAnyRole('TEACHER')";
    // Not READ_ROLES: ExcelService queries the repository directly and so is not
    // redacted by the guard, which would hand every teacher the whole staff list
    // with salaries attached. Bulk export stays with the roles entitled to that data.
    private static final String BULK_EXPORT_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','ACCOUNTANT')";
    // ID card printing: admin + HR-ish roles (accountant handles payroll/HR data in this
    // system, so it is included alongside the core admin roles), per the bonus-round spec.
    private static final String ID_CARD_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','ACCOUNTANT')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List teachers, paginated, with optional search/department/designation/status filters")
    public ResponseEntity<ApiResponse<PageResponse<TeacherDto>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long designationId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        return ResponseEntity.ok(ApiResponse.success("Teachers retrieved successfully",
                teacherService.getAll(search, departmentId, designationId, status, page, size, sortBy, sortDirection)));
    }

    @GetMapping("/me")
    @PreAuthorize(SELF_SERVICE_ROLES)
    @Operation(summary = "Get the signed-in teacher's own profile, unredacted (no id to tamper with)")
    public ResponseEntity<ApiResponse<TeacherDto>> getOwnProfile() {
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", teacherService.getOwnProfile()));
    }

    @PatchMapping("/me")
    @PreAuthorize(SELF_SERVICE_ROLES)
    @Operation(summary = "Update the contact and qualification details a teacher may maintain themselves")
    public ResponseEntity<ApiResponse<TeacherDto>> updateOwnProfile(
            @Valid @RequestBody TeacherSelfUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully",
                teacherService.updateOwnProfile(request)));
    }

    @GetMapping("/me/assignments")
    @PreAuthorize(SELF_SERVICE_ROLES)
    @Operation(summary = "List the classes, sections and subjects the signed-in teacher is assigned to")
    public ResponseEntity<ApiResponse<List<TeacherAssignmentDto>>> getOwnAssignments() {
        return ResponseEntity.ok(ApiResponse.success("Assignments retrieved successfully",
                teacherService.getOwnAssignments()));
    }

    // Mapped after /me so Spring never treats the literal "me" as an {id} candidate.
    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Get a teacher's full profile, including subject/section assignments")
    public ResponseEntity<ApiResponse<TeacherDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Teacher retrieved successfully", teacherService.getById(id)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create a new teacher (creates the login account and profile in one transaction)")
    public ResponseEntity<ApiResponse<TeacherDto>> create(@Valid @RequestBody TeacherCreateRequest request) {
        TeacherDto created = teacherService.create(request);
        URI location = URI.create("/api/v1/teachers/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Teacher created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a teacher's profile")
    public ResponseEntity<ApiResponse<TeacherDto>> update(@PathVariable Long id,
                                                           @Valid @RequestBody TeacherUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Teacher updated successfully", teacherService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Soft-delete a teacher and deactivate their login account")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teacherService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a teacher's employment status")
    public ResponseEntity<ApiResponse<TeacherDto>> updateStatus(@PathVariable Long id,
                                                                 @Valid @RequestBody TeacherStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Teacher status updated successfully", teacherService.updateStatus(id, request)));
    }

    @GetMapping("/{id}/id-card/pdf")
    @PreAuthorize(ID_CARD_ROLES)
    @Operation(summary = "Download a printable teacher/staff ID card (with QR code) as a PDF")
    public ResponseEntity<byte[]> getIdCardPdf(@PathVariable Long id) {
        byte[] pdf = pdfService.generateTeacherIdCardPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("teacher-id-card-" + id + ".pdf").build().toString())
                .body(pdf);
    }

    @GetMapping("/export/excel")
    @PreAuthorize(BULK_EXPORT_ROLES)
    @Operation(summary = "Export the full teacher list as an Excel (.xlsx) workbook")
    public ResponseEntity<byte[]> exportExcel() {
        byte[] workbook = excelService.exportTeachers();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("teachers.xlsx").build().toString())
                .body(workbook);
    }
}
