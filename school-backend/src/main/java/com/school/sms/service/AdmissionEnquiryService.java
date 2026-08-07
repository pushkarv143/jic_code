package com.school.sms.service;

import com.school.sms.dto.request.AdmissionEnquiryRequest;
import com.school.sms.dto.request.AdmissionStatusRequest;
import com.school.sms.dto.response.AdmissionEnquiryDto;
import com.school.sms.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AdmissionEnquiryService {

    AdmissionEnquiryDto create(AdmissionEnquiryRequest request);

    PageResponse<AdmissionEnquiryDto> getAll(String status, Pageable pageable);

    AdmissionEnquiryDto updateStatus(Long id, AdmissionStatusRequest request);

    void delete(Long id);
}
