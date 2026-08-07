package com.school.sms.service;

import com.school.sms.dto.request.SchoolClassRequest;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.SchoolClassDto;

public interface SchoolClassService {

    PageResponse<SchoolClassDto> getAll(Long academicYearId, int page, int size, String sortBy, String sortDirection);

    SchoolClassDto getById(Long id);

    SchoolClassDto create(SchoolClassRequest request);

    SchoolClassDto update(Long id, SchoolClassRequest request);

    void delete(Long id);
}
