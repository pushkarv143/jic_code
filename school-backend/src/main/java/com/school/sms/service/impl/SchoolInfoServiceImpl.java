package com.school.sms.service.impl;

import com.school.sms.dto.request.SchoolInfoRequest;
import com.school.sms.dto.response.SchoolInfoDto;
import com.school.sms.entity.SchoolInfo;
import com.school.sms.repository.SchoolInfoRepository;
import com.school.sms.service.SchoolInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SchoolInfoServiceImpl implements SchoolInfoService {

    private static final long SINGLETON_ID = 1L;

    private final SchoolInfoRepository schoolInfoRepository;

    @Override
    @Transactional
    public SchoolInfoDto get() {
        return toDto(getOrCreate());
    }

    @Override
    @Transactional
    public SchoolInfoDto update(SchoolInfoRequest request) {
        SchoolInfo info = getOrCreate();
        info.setName(request.getName());
        info.setAddress(request.getAddress());
        info.setPhone(request.getPhone());
        info.setEmail(request.getEmail());
        info.setLogoUrl(request.getLogoUrl());
        info.setEstablishedYear(request.getEstablishedYear());
        info.setAffiliationNumber(request.getAffiliationNumber());
        return toDto(schoolInfoRepository.save(info));
    }

    /**
     * Singleton row: id=1 by convention (the first and only row ever
     * inserted). Created on first read/write with empty fields so the
     * frontend always has something to render/edit rather than a 404.
     */
    private SchoolInfo getOrCreate() {
        return schoolInfoRepository.findById(SINGLETON_ID)
                .orElseGet(() -> schoolInfoRepository.save(SchoolInfo.builder()
                        .name("")
                        .address("")
                        .phone("")
                        .email("")
                        .logoUrl("")
                        .establishedYear(null)
                        .affiliationNumber("")
                        .build()));
    }

    private SchoolInfoDto toDto(SchoolInfo info) {
        return SchoolInfoDto.builder()
                .id(info.getId())
                .name(info.getName())
                .address(info.getAddress())
                .phone(info.getPhone())
                .email(info.getEmail())
                .logoUrl(info.getLogoUrl())
                .establishedYear(info.getEstablishedYear())
                .affiliationNumber(info.getAffiliationNumber())
                .build();
    }
}
