package com.school.sms.service;

import com.school.sms.dto.request.HostelStudentRequest;
import com.school.sms.dto.request.HostelStudentVacateRequest;
import com.school.sms.dto.response.HostelStudentDto;
import com.school.sms.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface HostelStudentService {

    PageResponse<HostelStudentDto> getAll(Long studentId, Long roomId, String status, Pageable pageable);

    HostelStudentDto allocate(HostelStudentRequest request);

    HostelStudentDto vacate(Long id, HostelStudentVacateRequest request);
}
