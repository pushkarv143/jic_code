package com.school.sms.service;

import com.school.sms.dto.response.ClassOverviewDto;
import com.school.sms.dto.response.ClassTeacherAvailabilityDto;
import com.school.sms.dto.response.TeacherWorkloadDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ClassOverviewService {

    /**
     * Everything the Class Overview screen shows for one class.
     *
     * @param startDate start of the range the stats cover; defaults to the start
     *                  of the current month when null
     * @param endDate   end of that range; defaults to today when null
     */
    ClassOverviewDto getOverview(Long classId, LocalDate startDate, LocalDate endDate);

    /** Active student counts keyed by class id, for the class list. */
    Map<Long, Integer> countStudentsByClass();

    /** Active student counts keyed by section id. */
    Map<Long, Integer> countStudentsBySection();

    /** Teachers who already have a homeroom, and where. Anyone absent is free. */
    List<ClassTeacherAvailabilityDto> getClassTeacherAvailability();

    /** School-wide teacher workload, heaviest first. */
    List<TeacherWorkloadDto> getTeacherWorkload();
}
