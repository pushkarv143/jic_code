package com.school.sms.service;

import com.school.sms.dto.request.FeeStructureRequest;
import com.school.sms.dto.response.FeeStructureDto;

import java.util.List;

public interface FeeStructureService {

    List<FeeStructureDto> getAll(Long classId, Long academicYearId, Long feeCategoryId);

    FeeStructureDto create(FeeStructureRequest request);

    FeeStructureDto update(Long id, FeeStructureRequest request);

    void delete(Long id);
}
