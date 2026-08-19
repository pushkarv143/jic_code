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

    /**
     * The signed-in caller's own week, with no id in the path to tamper with: a
     * teacher gets the periods they teach, a student the week of the class they
     * are enrolled in, and a parent of one child that child's week.
     */
    List<TimetableSlotDto> getForCurrentUser();

    /**
     * The week of the class a given student is enrolled in.
     *
     * <p>Separate from {@link #getForCurrentUser()} so a parent of several
     * children can ask about one of them by id; self-scoped callers are still
     * held to their own children.
     */
    List<TimetableSlotDto> getForStudent(Long studentId);

    /** Replaces the section's whole week in one transaction. */
    List<TimetableSlotDto> saveForSection(Long classId, Long sectionId, SaveTimetableRequest request);
}
