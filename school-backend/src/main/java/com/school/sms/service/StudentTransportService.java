package com.school.sms.service;

import com.school.sms.dto.request.StudentTransportRequest;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudentTransportDto;
import org.springframework.data.domain.Pageable;

public interface StudentTransportService {

    PageResponse<StudentTransportDto> getAll(Long studentId, Long routeId, Pageable pageable);

    StudentTransportDto create(StudentTransportRequest request);

    StudentTransportDto update(Long id, StudentTransportRequest request);

    void delete(Long id);
}
