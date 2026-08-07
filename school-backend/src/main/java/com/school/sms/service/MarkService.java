package com.school.sms.service;

import com.school.sms.dto.request.MarkEntryRequest;
import com.school.sms.dto.response.MarkDto;
import com.school.sms.dto.response.MarkRosterRowDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.ReportCardDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MarkService {

    PageResponse<MarkDto> getAll(Long examScheduleId, Long studentId, Pageable pageable);

    /**
     * Every active student in the exam schedule's class (across all its sections),
     * with their existing mark (or null if not yet entered) — powers the marks-entry
     * grid, same "roster with nullable status" shape as the attendance module's
     * {@code GET /attendance/students}.
     */
    List<MarkRosterRowDto> getRoster(Long examScheduleId);

    List<MarkDto> enterMarks(MarkEntryRequest request);

    ReportCardDto getReportCard(Long studentId, Long examId);
}
