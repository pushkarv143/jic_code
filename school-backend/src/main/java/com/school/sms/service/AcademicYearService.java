package com.school.sms.service;

import com.school.sms.dto.request.AcademicYearRequest;
import com.school.sms.dto.response.AcademicYearDto;

import java.util.List;

public interface AcademicYearService {

    List<AcademicYearDto> getAll();

    AcademicYearDto getById(Long id);

    AcademicYearDto create(AcademicYearRequest request);

    AcademicYearDto update(Long id, AcademicYearRequest request);

    void delete(Long id);

    AcademicYearDto setCurrent(Long id);
}
