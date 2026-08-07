package com.school.sms.controller;

import com.school.sms.dto.request.FeePaymentRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.FeePaymentDto;
import com.school.sms.dto.response.FeePaymentResultDto;
import com.school.sms.dto.response.FeeReceiptDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.service.FeePaymentService;
import com.school.sms.service.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/fee-payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fee Payments", description = "Collect fee payments and print receipts")
public class FeePaymentController {

    private final FeePaymentService feePaymentService;
    private final PdfService pdfService;

    // STUDENT/PARENT are included; StudentAccessGuard scopes them to their own record.
    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','ACCOUNTANT','STUDENT','PARENT')";
    private static final String COLLECT_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','ACCOUNTANT')";

    @PostMapping
    @PreAuthorize(COLLECT_ROLES)
    @Operation(summary = "Record a fee payment; the amount_paid/status roll-up is handled by a DB trigger")
    public ResponseEntity<ApiResponse<FeePaymentResultDto>> pay(@Valid @RequestBody FeePaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment recorded successfully", feePaymentService.pay(request)));
    }

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Paginated payment history, filterable by student/date range")
    public ResponseEntity<ApiResponse<PageResponse<FeePaymentDto>>> getAll(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Payment history retrieved successfully",
                feePaymentService.getAll(studentId, startDate, endDate, pageable)));
    }

    @GetMapping("/{id}/receipt")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Structured receipt data for a payment, for the frontend to render/print")
    public ResponseEntity<ApiResponse<FeeReceiptDto>> getReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Receipt retrieved successfully", feePaymentService.getReceipt(id)));
    }

    @GetMapping("/{id}/receipt/pdf")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Download the fee payment receipt as a printable PDF")
    public ResponseEntity<byte[]> getReceiptPdf(@PathVariable Long id) {
        FeeReceiptDto receipt = feePaymentService.getReceipt(id);
        byte[] pdf = pdfService.generateFeeReceiptPdf(id);
        String filename = "receipt-" + receipt.getReceiptNumber() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(pdf);
    }
}
