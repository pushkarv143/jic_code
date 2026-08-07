package com.school.sms.service;

import com.school.sms.dto.request.ExamRequest;
import com.school.sms.dto.request.ExamScheduleRequest;
import com.school.sms.dto.response.ExamDto;
import com.school.sms.dto.response.ExamResultRowDto;
import com.school.sms.dto.response.ExamScheduleDto;
import com.school.sms.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ExamService {

    PageResponse<ExamDto> getAll(Long classId, Long academicYearId, Long examTypeId, Pageable pageable);

    ExamDto getById(Long id);

    ExamDto create(ExamRequest request);

    ExamDto update(Long id, ExamRequest request);

    void delete(Long id);

    List<ExamScheduleDto> getSchedules(Long examId);

    ExamScheduleDto addSchedule(Long examId, ExamScheduleRequest request);

    ExamScheduleDto updateSchedule(Long scheduleId, ExamScheduleRequest request);

    void deleteSchedule(Long scheduleId);

    List<ExamResultRowDto> getResults(Long examId, Long classId, Long sectionId);
}
