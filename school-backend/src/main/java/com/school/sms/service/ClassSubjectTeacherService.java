package com.school.sms.service;

import com.school.sms.dto.request.ClassSubjectTeacherRequest;
import com.school.sms.dto.response.ClassSubjectTeacherDto;

import java.util.List;

public interface ClassSubjectTeacherService {

    List<ClassSubjectTeacherDto> getAll(Long sectionId, Long subjectId, Long teacherId);

    /**
     * What the signed-in caller is entitled to see of the subject/teacher grid: a
     * teacher gets the subjects they have been assigned, a student the subjects of
     * the class they are enrolled in — which is how either finds out who teaches
     * what without being able to read the whole mapping table.
     */
    List<ClassSubjectTeacherDto> getForCurrentUser();

    /** The subject/teacher list for the class a given student is enrolled in. */
    List<ClassSubjectTeacherDto> getForStudent(Long studentId);

    ClassSubjectTeacherDto create(ClassSubjectTeacherRequest request);

    void delete(Long id);
}
