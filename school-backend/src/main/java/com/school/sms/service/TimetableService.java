package com.school.sms.service;

import com.school.sms.dto.request.SaveTimetableRequest;
import com.school.sms.dto.response.TimetableSlotDto;

import java.util.List;

public interface TimetableService {

    /** One section's week, clash warnings already attached to the offending slots. */
    List<TimetableSlotDto> getForSection(Long classId, Long sectionId);

    /** Every section of a class, for the class-wide grid. */
    List<TimetableSlotDto> getForClass(Long classId);

    /** One teacher's week across every section they are timetabled in. */
    List<TimetableSlotDto> getForTeacher(Long teacherId);

    /** Replaces the section's whole week in one transaction. */
    List<TimetableSlotDto> saveForSection(Long classId, Long sectionId, SaveTimetableRequest request);
}
