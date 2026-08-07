package com.school.sms.service;

import com.school.sms.dto.request.FeeCategoryRequest;
import com.school.sms.dto.response.FeeCategoryDto;

import java.util.List;

public interface FeeCategoryService {

    List<FeeCategoryDto> getAll();

    FeeCategoryDto create(FeeCategoryRequest request);

    FeeCategoryDto update(Long id, FeeCategoryRequest request);

    void delete(Long id);
}
