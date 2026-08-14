package com.school.sms.service;

import com.school.sms.dto.request.TeacherCreateRequest;
import com.school.sms.dto.request.TeacherSelfUpdateRequest;
import com.school.sms.dto.request.TeacherStatusRequest;
import com.school.sms.dto.request.TeacherUpdateRequest;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.TeacherAssignmentDto;
import com.school.sms.dto.response.TeacherDto;

import java.util.List;

public interface TeacherService {

    PageResponse<TeacherDto> getAll(String search, Long departmentId, Long designationId, String status,
                                     int page, int size, String sortBy, String sortDirection);

    TeacherDto getById(Long id);

    /** The caller's own teacher record, resolved from the security context. */
    TeacherDto getOwnProfile();

    /** Updates the contact/qualification fields a teacher may maintain themselves. */
    TeacherDto updateOwnProfile(TeacherSelfUpdateRequest request);

    /** The class/section/subject rows the caller is assigned to teach. */
    List<TeacherAssignmentDto> getOwnAssignments();

    TeacherDto create(TeacherCreateRequest request);

    TeacherDto update(Long id, TeacherUpdateRequest request);

    void delete(Long id);

    TeacherDto updateStatus(Long id, TeacherStatusRequest request);
}
