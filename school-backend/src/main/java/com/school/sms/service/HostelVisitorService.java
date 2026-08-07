package com.school.sms.service;

import com.school.sms.dto.request.HostelVisitorRequest;
import com.school.sms.dto.response.HostelVisitorDto;
import com.school.sms.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface HostelVisitorService {

    PageResponse<HostelVisitorDto> getAll(Long studentId, Pageable pageable);

    HostelVisitorDto checkIn(HostelVisitorRequest request);

    HostelVisitorDto checkOut(Long id);
}
