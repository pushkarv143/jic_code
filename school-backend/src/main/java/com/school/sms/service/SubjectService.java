package com.school.sms.service;

import com.school.sms.dto.request.SubjectRequest;
import com.school.sms.dto.response.SubjectDto;

import java.util.List;

public interface SubjectService {

    List<SubjectDto> getByClassId(Long classId);

    SubjectDto create(Long classId, SubjectRequest request);

    SubjectDto update(Long id, SubjectRequest request);

    void delete(Long id);
}
