package com.school.sms.service;

import com.school.sms.dto.request.DesignationRequest;
import com.school.sms.dto.response.DesignationDto;

import java.util.List;

public interface DesignationService {

    List<DesignationDto> getAll();

    DesignationDto getById(Long id);

    DesignationDto create(DesignationRequest request);

    DesignationDto update(Long id, DesignationRequest request);

    void delete(Long id);
}
