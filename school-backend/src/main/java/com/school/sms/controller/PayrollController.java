package com.school.sms.controller;

import com.school.sms.dto.request.PayrollGenerateRequest;
import com.school.sms.dto.request.PayrollMarkPaidRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.PayrollDashboardDto;
import com.school.sms.dto.response.PayrollDto;
import com.school.sms.dto.response.PayrollGenerateResultDto;
import com.school.sms.dto.response.SalarySlipDto;
import com.school.sms.entity.PayrollEmployeeType;
import com.school.sms.service.PayrollService;
import com.school.sms.service.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payroll", description = "Monthly payroll generation, payment tracking and salary slips")
public class PayrollController {

    private final PayrollService payrollService;
    private final PdfService pdfService;

    private static final String READ_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','ACCOUNTANT')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','ACCOUNTANT')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Paginated payroll list, filterable by employee id/type/month/year/status")
    public ResponseEntity<ApiResponse<PageResponse<PayrollDto>>> getAll(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) PayrollEmployeeType employeeType,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Payroll records retrieved successfully",
                payrollService.getAll(employeeId, employeeType, month, year, status, pageable)));
    }

    @PostMapping("/generate")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Generate PENDING payroll rows for every active employee of the given type/month/year "
            + "that has a salary structure; existing employee+month+year rows are skipped")
    public ResponseEntity<ApiResponse<PayrollGenerateResultDto>> generate(
            @Valid @RequestBody PayrollGenerateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payroll generated successfully", payrollService.generate(request)));
    }

    @PatchMapping("/{id}/mark-paid")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Mark a payroll row as PAID with the given payment date")
    public ResponseEntity<ApiResponse<PayrollDto>> markPaid(@PathVariable Long id,
                                                             @Valid @RequestBody PayrollMarkPaidRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payroll marked as paid successfully",
                payrollService.markPaid(id, request)));
    }

    @GetMapping("/{id}/salary-slip")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Structured salary slip data for a payroll row, for the frontend to render/print")
    public ResponseEntity<ApiResponse<SalarySlipDto>> getSalarySlip(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Salary slip retrieved successfully", payrollService.getSalarySlip(id)));
    }

    @GetMapping("/{id}/salary-slip/pdf")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Download the salary slip for a payroll row as a printable PDF")
    public ResponseEntity<byte[]> getSalarySlipPdf(@PathVariable Long id) {
        byte[] pdf = pdfService.generateSalarySlipPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("salary-slip-" + id + ".pdf").build().toString())
                .body(pdf);
    }

    @GetMapping("/dashboard")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Aggregate total paid/pending amounts and employee count for a given month/year")
    public ResponseEntity<ApiResponse<PayrollDashboardDto>> getDashboard(
            @RequestParam Integer month, @RequestParam Integer year) {
        return ResponseEntity.ok(ApiResponse.success("Payroll dashboard retrieved successfully",
                payrollService.getDashboard(month, year)));
    }
}
