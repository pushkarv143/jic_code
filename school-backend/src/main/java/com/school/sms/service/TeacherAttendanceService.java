package com.school.sms.service;

import com.school.sms.dto.request.MarkTeacherAttendanceRequest;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.TeacherAttendanceRecordDto;
import com.school.sms.dto.response.TeacherAttendanceRowDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface TeacherAttendanceService {

    List<TeacherAttendanceRowDto> getGrid(LocalDate date);

    int mark(MarkTeacherAttendanceRequest request);

    PageResponse<TeacherAttendanceRecordDto> getReport(Long teacherId, LocalDate startDate, LocalDate endDate, Pageable pageable);
}
