package com.school.sms.controller;

import com.school.sms.dto.request.MarkEntryRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.MarkDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.ReportCardDto;
import com.school.sms.service.MarkService;
import com.school.sms.service.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/marks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Marks", description = "Enter marks, list them and generate student report cards")
public class MarkController {

    private final MarkService markService;
    private final PdfService pdfService;

    // STUDENT/PARENT are included; StudentAccessGuard scopes them to their own marks.
    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','RECEPTIONIST','ACCOUNTANT','STUDENT','PARENT')";
    private static final String ENTRY_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List marks, paginated, filterable by exam schedule/student")
    public ResponseEntity<ApiResponse<PageResponse<MarkDto>>> getAll(
            @RequestParam(required = false) Long examScheduleId,
            @RequestParam(required = false) Long studentId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Marks retrieved successfully",
                markService.getAll(examScheduleId, studentId, pageable)));
    }

    @GetMapping("/roster")
    @PreAuthorize(ENTRY_ROLES)
    @Operation(summary = "Every active student in an exam schedule's class, with their existing mark if any — powers the marks-entry grid")
    public ResponseEntity<ApiResponse<List<com.school.sms.dto.response.MarkRosterRowDto>>> getRoster(
            @RequestParam Long examScheduleId) {
        return ResponseEntity.ok(ApiResponse.success("Roster retrieved successfully", markService.getRoster(examScheduleId)));
    }

    @PostMapping("/entry")
    @PreAuthorize(ENTRY_ROLES)
    @Operation(summary = "Enter/update marks for every student in one exam schedule (upserts per student)")
    public ResponseEntity<ApiResponse<List<MarkDto>>> enterMarks(@Valid @RequestBody MarkEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Marks saved successfully", markService.enterMarks(request)));
    }

    @GetMapping("/report-card/{studentId}")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Generate a student's report card for one exam")
    public ResponseEntity<ApiResponse<ReportCardDto>> getReportCard(@PathVariable Long studentId,
                                                                     @RequestParam Long examId) {
        return ResponseEntity.ok(ApiResponse.success("Report card retrieved successfully",
                markService.getReportCard(studentId, examId)));
    }

    @GetMapping("/report-card/{studentId}/pdf")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Download a student's report card for one exam as a printable PDF")
    public ResponseEntity<byte[]> getReportCardPdf(@PathVariable Long studentId, @RequestParam Long examId) {
        byte[] pdf = pdfService.generateReportCardPdf(studentId, examId);
        String filename = "report-card-" + studentId + "-" + examId + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(pdf);
    }
}
