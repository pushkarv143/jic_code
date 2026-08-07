package com.school.sms.service;

import com.school.sms.dto.request.OnlineClassRequest;
import com.school.sms.dto.response.OnlineClassDto;
import com.school.sms.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface OnlineClassService {

    PageResponse<OnlineClassDto> getAll(Long classId, Long sectionId, Long subjectId, Long teacherId,
                                         Boolean upcoming, Pageable pageable);

    OnlineClassDto create(OnlineClassRequest request);

    OnlineClassDto update(Long id, OnlineClassRequest request);

    void delete(Long id);
}
