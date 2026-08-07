package com.school.sms.service.impl;

import com.school.sms.dto.request.AdmissionEnquiryRequest;
import com.school.sms.dto.request.AdmissionStatusRequest;
import com.school.sms.dto.response.AdmissionEnquiryDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.entity.AdmissionEnquiry;
import com.school.sms.entity.EnquiryStatus;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.AdmissionEnquiryRepository;
import com.school.sms.service.AdmissionEnquiryService;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdmissionEnquiryServiceImpl implements AdmissionEnquiryService {

    private final AdmissionEnquiryRepository admissionEnquiryRepository;

    @Override
    @Transactional
    public AdmissionEnquiryDto create(AdmissionEnquiryRequest request) {
        AdmissionEnquiry enquiry = AdmissionEnquiry.builder()
                .studentName(request.getStudentName())
                .parentName(request.getParentName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .classApplying(request.getClassApplying())
                .dob(request.getDob())
                .address(request.getAddress())
                .status(EnquiryStatus.PENDING)
                .appliedAt(LocalDateTime.now())
                .build();

        return toDto(admissionEnquiryRepository.save(enquiry));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdmissionEnquiryDto> getAll(String status, Pageable pageable) {
        Specification<AdmissionEnquiry> spec = new SpecificationBuilder<AdmissionEnquiry>()
                .with(StringUtils.hasText(status), "status", SearchOperation.EQUALS,
                        StringUtils.hasText(status) ? EnquiryStatus.valueOf(status.toUpperCase()) : null)
                .build();

        Page<AdmissionEnquiry> page = admissionEnquiryRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toDto).toList());
    }

    @Override
    @Transactional
    public AdmissionEnquiryDto updateStatus(Long id, AdmissionStatusRequest request) {
        AdmissionEnquiry enquiry = findEntity(id);
        EnquiryStatus status;
        try {
            status = EnquiryStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status: " + request.getStatus());
        }
        if (status == EnquiryStatus.PENDING) {
            throw new BadRequestException("Status can only be updated to APPROVED or REJECTED");
        }
        enquiry.setStatus(status);
        return toDto(admissionEnquiryRepository.save(enquiry));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        admissionEnquiryRepository.delete(findEntity(id));
    }

    private AdmissionEnquiryDto toDto(AdmissionEnquiry enquiry) {
        return AdmissionEnquiryDto.builder()
                .id(enquiry.getId())
                .studentName(enquiry.getStudentName())
                .parentName(enquiry.getParentName())
                .phone(enquiry.getPhone())
                .email(enquiry.getEmail())
                .classApplying(enquiry.getClassApplying())
                .dob(enquiry.getDob())
                .address(enquiry.getAddress())
                .status(enquiry.getStatus().name())
                .documentsUrl(enquiry.getDocumentsUrl())
                .appliedAt(enquiry.getAppliedAt())
                .build();
    }

    private AdmissionEnquiry findEntity(Long id) {
        return admissionEnquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdmissionEnquiry", "id", id));
    }
}
