package com.school.sms.service;

import com.school.sms.dto.request.MyClassOfficialRequest;
import com.school.sms.dto.request.MyClassStudentUpdateRequest;
import com.school.sms.dto.response.ClassOfficialDto;
import com.school.sms.dto.response.HomeroomDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudentDto;

import java.util.List;

/**
 * The homeroom teacher's view of their own section.
 *
 * <p>Every method resolves the section from {@code sections.class_teacher_id} for
 * the current caller. No method accepts a class or section id — see
 * {@link MyClassStudentUpdateRequest} for why the request types omit them rather
 * than validating them. That is the whole security model of this module: there is
 * no parameter a class teacher can change to reach a section other than their own.
 */
public interface MyClassService {

    /** The caller's homeroom section. */
    HomeroomDto getMyClass();

    /** The caller's homeroom roster. */
    PageResponse<StudentDto> getMyStudents(String search, String status, int page, int size,
                                           String sortBy, String sortDirection);

    /** Updates a student who is on the caller's own roster. */
    StudentDto updateStudent(Long studentId, MyClassStudentUpdateRequest request);

    /** Current holders of every post in the caller's own class. */
    List<ClassOfficialDto> getOfficials();

    /** Appoints one of the caller's own students to a post in their class. */
    ClassOfficialDto appointOfficial(MyClassOfficialRequest request);

    /** Ends an appointment in the caller's own class, leaving the post vacant. */
    void endOfficial(Long officialId);
}
