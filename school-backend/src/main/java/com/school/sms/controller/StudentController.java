package com.school.sms.controller;

import com.school.sms.dto.request.GuardianRequest;
import com.school.sms.dto.request.MedicalDetailsRequest;
import com.school.sms.dto.request.PromoteStudentsRequest;
import com.school.sms.dto.request.StudentCreateRequest;
import com.school.sms.dto.request.StudentSelfUpdateRequest;
import com.school.sms.dto.request.StudentStatusRequest;
import com.school.sms.dto.request.StudentUpdateRequest;
import com.school.sms.dto.request.TransferStudentRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.GuardianDto;
import com.school.sms.dto.response.MedicalDetailsDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudentDocumentDto;
import com.school.sms.dto.response.ExcelImportResultDto;
import com.school.sms.dto.response.StudentDto;
import com.school.sms.service.ExcelService;
import com.school.sms.service.PdfService;
import com.school.sms.service.StudentService;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Students", description = "Manage student admissions, guardians, medical details and documents")
public class StudentController {

    private final StudentService studentService;
    private final PdfService pdfService;
    private final ExcelService excelService;

    // Who may call the read endpoints at all. STUDENT/PARENT are included because
    // they have a legitimate view of student data — but only of their own record;
    // which rows they actually get back is decided by StudentAccessGuard inside the
    // service, which likewise narrows TEACHER/CLASS_TEACHER to the students they
    // teach. Role here, row-level scope there: neither check substitutes for the other.
    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','CLASS_TEACHER','RECEPTIONIST','ACCOUNTANT','STUDENT','PARENT')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";
    // Self-service: a student maintains their own contact details. PARENT is excluded
    // because a parent has no single "own" record — they use the by-id endpoints,
    // which the guard already narrows to their own children.
    private static final String SELF_SERVICE_ROLES = "hasRole('STUDENT')";
    private static final String BULK_EXPORT_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','RECEPTIONIST','ACCOUNTANT')";
    // Photo/document uploads are also allowed for TEACHER/CLASS_TEACHER (e.g. a homeroom
    // teacher completing a student's file); we do not further restrict this to only the
    // student's own homeroom teacher — see deviations note in the round report.
    private static final String UPLOAD_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','CLASS_TEACHER')";
    // ID card printing: front-desk/admin roles only, per the bonus-round spec.
    private static final String ID_CARD_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','RECEPTIONIST')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List students, paginated, with optional search/class/section/status filters")
    public ResponseEntity<ApiResponse<PageResponse<StudentDto>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        return ResponseEntity.ok(ApiResponse.success("Students retrieved successfully",
                studentService.getAll(search, classId, sectionId, status, page, size, sortBy, sortDirection)));
    }

    @GetMapping("/me")
    @PreAuthorize(SELF_SERVICE_ROLES)
    @Operation(summary = "Get the signed-in student's own profile (no id to tamper with)")
    public ResponseEntity<ApiResponse<StudentDto>> getOwnProfile() {
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", studentService.getOwnProfile()));
    }

    @PatchMapping("/me")
    @PreAuthorize(SELF_SERVICE_ROLES)
    @Operation(summary = "Update the contact details a student is permitted to maintain themselves")
    public ResponseEntity<ApiResponse<StudentDto>> updateOwnProfile(
            @Valid @RequestBody StudentSelfUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully",
                studentService.updateOwnProfile(request)));
    }

    // Mapped after /me so Spring never treats the literal "me" as an {id} candidate.
    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Get a student's full profile, including guardians, medical details and documents")
    public ResponseEntity<ApiResponse<StudentDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Student retrieved successfully", studentService.getById(id)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Admit a new student (login account is optional; guardians are created in the same transaction)")
    public ResponseEntity<ApiResponse<StudentDto>> create(@Valid @RequestBody StudentCreateRequest request) {
        StudentDto created = studentService.create(request);
        URI location = URI.create("/api/v1/students/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Student created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a student's profile")
    public ResponseEntity<ApiResponse<StudentDto>> update(@PathVariable Long id,
                                                           @Valid @RequestBody StudentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Student updated successfully", studentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Soft-delete a student")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a student's status")
    public ResponseEntity<ApiResponse<StudentDto>> updateStatus(@PathVariable Long id,
                                                                 @Valid @RequestBody StudentStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Student status updated successfully", studentService.updateStatus(id, request)));
    }

    @PostMapping(value = "/{id}/photo", consumes = "multipart/form-data")
    @PreAuthorize(UPLOAD_ROLES)
    @Operation(summary = "Upload/replace a student's photo")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadPhoto(@PathVariable Long id,
                                                                         @RequestPart("file") MultipartFile file) {
        String url = studentService.uploadPhoto(id, file);
        return ResponseEntity.ok(ApiResponse.success("Photo uploaded successfully", Map.of("photoUrl", url)));
    }

    @GetMapping("/{id}/guardians")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List a student's guardians")
    public ResponseEntity<ApiResponse<List<GuardianDto>>> getGuardians(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Guardians retrieved successfully", studentService.getGuardians(id)));
    }

    @PostMapping("/{id}/guardians")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Add a guardian to a student")
    public ResponseEntity<ApiResponse<GuardianDto>> addGuardian(@PathVariable Long id,
                                                                 @Valid @RequestBody GuardianRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Guardian added successfully", studentService.addGuardian(id, request)));
    }

    @PutMapping("/{id}/guardians/{guardianId}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a student's guardian")
    public ResponseEntity<ApiResponse<GuardianDto>> updateGuardian(@PathVariable Long id, @PathVariable Long guardianId,
                                                                    @Valid @RequestBody GuardianRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Guardian updated successfully",
                studentService.updateGuardian(id, guardianId, request)));
    }

    @DeleteMapping("/{id}/guardians/{guardianId}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Remove a student's guardian")
    public ResponseEntity<Void> deleteGuardian(@PathVariable Long id, @PathVariable Long guardianId) {
        studentService.deleteGuardian(id, guardianId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/medical-details")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Get a student's medical details, if recorded")
    public ResponseEntity<ApiResponse<MedicalDetailsDto>> getMedicalDetails(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Medical details retrieved successfully", studentService.getMedicalDetails(id)));
    }

    @PutMapping("/{id}/medical-details")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create or update a student's medical details")
    public ResponseEntity<ApiResponse<MedicalDetailsDto>> upsertMedicalDetails(@PathVariable Long id,
                                                                                @Valid @RequestBody MedicalDetailsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Medical details saved successfully",
                studentService.upsertMedicalDetails(id, request)));
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List a student's uploaded documents")
    public ResponseEntity<ApiResponse<List<StudentDocumentDto>>> getDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", studentService.getDocuments(id)));
    }

    @PostMapping(value = "/{id}/documents", consumes = "multipart/form-data")
    @PreAuthorize(UPLOAD_ROLES)
    @Operation(summary = "Upload a document for a student")
    public ResponseEntity<ApiResponse<StudentDocumentDto>> addDocument(@PathVariable Long id,
                                                                        @RequestPart("file") MultipartFile file,
                                                                        @RequestParam("documentType") String documentType) {
        StudentDocumentDto created = studentService.addDocument(id, documentType, file);
        return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", created));
    }

    @DeleteMapping("/{id}/documents/{docId}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete a student's document")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id, @PathVariable Long docId) {
        studentService.deleteDocument(id, docId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/promote")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Bulk-promote students to a new class/section/academic year")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> promote(@Valid @RequestBody PromoteStudentsRequest request) {
        int count = studentService.promote(request);
        return ResponseEntity.ok(ApiResponse.success(count + " student(s) promoted successfully", Map.of("promoted", count)));
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Mark a student as transferred out")
    public ResponseEntity<ApiResponse<Void>> transfer(@PathVariable Long id, @RequestBody(required = false) TransferStudentRequest request) {
        studentService.transfer(id, request != null ? request : new TransferStudentRequest());
        return ResponseEntity.ok(ApiResponse.success("Student marked as transferred"));
    }

    @PostMapping("/{id}/mark-alumni")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Mark a student as an alumnus")
    public ResponseEntity<ApiResponse<Void>> markAlumni(@PathVariable Long id) {
        studentService.markAlumni(id);
        return ResponseEntity.ok(ApiResponse.success("Student marked as alumni"));
    }

    @GetMapping("/{id}/id-card/pdf")
    @PreAuthorize(ID_CARD_ROLES)
    @Operation(summary = "Download a printable student ID card (with QR code) as a PDF")
    public ResponseEntity<byte[]> getIdCardPdf(@PathVariable Long id) {
        byte[] pdf = pdfService.generateStudentIdCardPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("student-id-card-" + id + ".pdf").build().toString())
                .body(pdf);
    }

    // Not READ_ROLES: ExcelService queries the repository directly and so is not
    // narrowed by StudentAccessGuard. Rather than let a student export the whole
    // directory, bulk export stays with the roles that are entitled to all rows.
    @GetMapping("/export/excel")
    @PreAuthorize(BULK_EXPORT_ROLES)
    @Operation(summary = "Export the filtered student list as an Excel (.xlsx) workbook")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String status) {
        byte[] workbook = excelService.exportStudents(classId, sectionId, status);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("students.xlsx").build().toString())
                .body(workbook);
    }

    @PostMapping(value = "/import/excel", consumes = "multipart/form-data")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Bulk-import students from an Excel (.xlsx) file; invalid rows are skipped and reported rather than failing the whole batch")
    public ResponseEntity<ApiResponse<ExcelImportResultDto>> importExcel(@RequestPart("file") MultipartFile file) {
        ExcelImportResultDto result = excelService.importStudents(file);
        return ResponseEntity.ok(ApiResponse.success(
                result.getImportedCount() + " student(s) imported successfully", result));
    }
}
