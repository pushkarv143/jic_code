package com.school.sms.service;

import com.school.sms.dto.request.AssignmentFormRequest;
import com.school.sms.dto.request.GradeSubmissionRequest;
import com.school.sms.dto.response.AssignmentDto;
import com.school.sms.dto.response.AssignmentSubmissionDto;
import com.school.sms.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AssignmentService {

    PageResponse<AssignmentDto> getAll(Long classId, Long sectionId, Long subjectId, Long teacherId, Pageable pageable);

    AssignmentDto create(AssignmentFormRequest request, MultipartFile file);

    AssignmentDto update(Long id, AssignmentFormRequest request, MultipartFile file);

    void delete(Long id);

    List<AssignmentSubmissionDto> getSubmissions(Long assignmentId);

    AssignmentSubmissionDto getMySubmission(Long assignmentId);

    AssignmentSubmissionDto submit(Long assignmentId, MultipartFile file);

    AssignmentSubmissionDto gradeSubmission(Long submissionId, GradeSubmissionRequest request);
}
