package com.school.sms.controller;

import com.school.sms.dto.request.AdmissionEnquiryRequest;
import com.school.sms.dto.response.AdmissionEnquiryDto;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.service.AdmissionEnquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, unauthenticated admission enquiry submission form. Matched by the
 * existing "/api/v1/public/**" permit-all rule in SecurityConfig — no new
 * security rule was needed (confirmed round-1's SecurityConfig already
 * permits this exact prefix).
 */
@RestController
@RequestMapping("/api/v1/public/admission-enquiries")
@RequiredArgsConstructor
@Tag(name = "Public Admission Enquiries", description = "Public admission enquiry submission form (no authentication required)")
public class PublicAdmissionEnquiryController {

    private final AdmissionEnquiryService admissionEnquiryService;

    @PostMapping
    @Operation(summary = "Submit a new admission enquiry (public, no authentication required)")
    public ResponseEntity<ApiResponse<AdmissionEnquiryDto>> create(@Valid @RequestBody AdmissionEnquiryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Admission enquiry submitted successfully", admissionEnquiryService.create(request)));
    }
}
