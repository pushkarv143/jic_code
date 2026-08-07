package com.school.sms.service;

import com.school.sms.dto.request.LeaveApplicationRequest;
import com.school.sms.dto.response.LeaveApplicationDto;
import com.school.sms.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface LeaveApplicationService {

    PageResponse<LeaveApplicationDto> getAll(String applicantType, String status, Pageable pageable);

    PageResponse<LeaveApplicationDto> getMy(Pageable pageable);

    LeaveApplicationDto create(LeaveApplicationRequest request);

    LeaveApplicationDto approve(Long id);

    LeaveApplicationDto reject(Long id);

    void delete(Long id);
}
