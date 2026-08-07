package com.school.sms.service;

import com.school.sms.dto.request.DepartmentRequest;
import com.school.sms.dto.response.DepartmentDto;

import java.util.List;

public interface DepartmentService {

    List<DepartmentDto> getAll();

    DepartmentDto getById(Long id);

    DepartmentDto create(DepartmentRequest request);

    DepartmentDto update(Long id, DepartmentRequest request);

    void delete(Long id);
}
