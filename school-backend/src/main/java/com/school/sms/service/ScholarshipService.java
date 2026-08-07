package com.school.sms.service;

import com.school.sms.dto.request.ScholarshipRequest;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.ScholarshipDto;
import org.springframework.data.domain.Pageable;

public interface ScholarshipService {

    PageResponse<ScholarshipDto> getAll(Long studentId, Long academicYearId, Pageable pageable);

    ScholarshipDto create(ScholarshipRequest request);

    ScholarshipDto update(Long id, ScholarshipRequest request);

    void delete(Long id);
}
