package com.school.sms.service;

import com.school.sms.dto.request.HostelFeeRequest;
import com.school.sms.dto.response.HostelFeeDto;
import com.school.sms.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface HostelFeeService {

    PageResponse<HostelFeeDto> getAll(Long studentId, Integer month, Integer year, String paidStatus, Pageable pageable);

    HostelFeeDto create(HostelFeeRequest request);

    HostelFeeDto markPaid(Long id);
}
