package com.school.sms.service;

import com.school.sms.dto.request.ClassSubjectTeacherRequest;
import com.school.sms.dto.response.ClassSubjectTeacherDto;

import java.util.List;

public interface ClassSubjectTeacherService {

    List<ClassSubjectTeacherDto> getAll(Long sectionId, Long subjectId, Long teacherId);

    ClassSubjectTeacherDto create(ClassSubjectTeacherRequest request);

    void delete(Long id);
}
