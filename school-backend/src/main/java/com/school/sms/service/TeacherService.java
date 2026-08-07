package com.school.sms.service;

import com.school.sms.dto.request.TeacherCreateRequest;
import com.school.sms.dto.request.TeacherStatusRequest;
import com.school.sms.dto.request.TeacherUpdateRequest;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.TeacherDto;

public interface TeacherService {

    PageResponse<TeacherDto> getAll(String search, Long departmentId, Long designationId, String status,
                                     int page, int size, String sortBy, String sortDirection);

    TeacherDto getById(Long id);

    TeacherDto create(TeacherCreateRequest request);

    TeacherDto update(Long id, TeacherUpdateRequest request);

    void delete(Long id);

    TeacherDto updateStatus(Long id, TeacherStatusRequest request);
}
