package com.school.sms.service;

import com.school.sms.dto.request.AssignClassTeacherRequest;
import com.school.sms.dto.request.SectionRequest;
import com.school.sms.dto.response.SectionDto;

import java.util.List;

public interface SectionService {

    List<SectionDto> getByClassId(Long classId);

    SectionDto create(Long classId, SectionRequest request);

    SectionDto update(Long id, SectionRequest request);

    void delete(Long id);

    SectionDto assignClassTeacher(Long id, AssignClassTeacherRequest request);
}
