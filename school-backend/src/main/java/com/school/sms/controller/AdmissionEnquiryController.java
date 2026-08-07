package com.school.sms.controller;

import com.school.sms.dto.request.AdmissionStatusRequest;
import com.school.sms.dto.response.AdmissionEnquiryDto;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.service.AdmissionEnquiryService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admission-enquiries")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admission Enquiries", description = "Review and process admission enquiries submitted via the public form")
public class AdmissionEnquiryController {

    private final AdmissionEnquiryService admissionEnquiryService;

    private static final String READ_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','RECEPTIONIST')";
    private static final String STATUS_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','RECEPTIONIST')";
    private static final String DELETE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List admission enquiries, paginated, optionally filtered by status")
    public ResponseEntity<ApiResponse<PageResponse<AdmissionEnquiryDto>>> getAll(
            @RequestParam(required = false) String status, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Admission enquiries retrieved successfully",
                admissionEnquiryService.getAll(status, pageable)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(STATUS_ROLES)
    @Operation(summary = "Approve or reject an admission enquiry")
    public ResponseEntity<ApiResponse<AdmissionEnquiryDto>> updateStatus(@PathVariable Long id,
                                                                          @Valid @RequestBody AdmissionStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Admission enquiry status updated successfully",
                admissionEnquiryService.updateStatus(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(DELETE_ROLES)
    @Operation(summary = "Delete an admission enquiry")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        admissionEnquiryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
