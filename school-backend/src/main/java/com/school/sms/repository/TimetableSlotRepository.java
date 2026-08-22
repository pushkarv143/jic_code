package com.school.sms.repository;

import com.school.sms.entity.TimetableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.List;

public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, Long> {

    List<TimetableSlot> findAllBySectionIdOrderByDayOfWeekAscPeriodNumberAsc(Long sectionId);

    List<TimetableSlot> findAllBySchoolClassIdOrderByDayOfWeekAscPeriodNumberAsc(Long classId);

    List<TimetableSlot> findAllByTeacherIdOrderByDayOfWeekAscPeriodNumberAsc(Long teacherId);

    void deleteAllBySectionId(Long sectionId);

    /**
     * Every (day, period) at which one teacher is booked into more than one
     * section. Grouped in SQL rather than scanned in Java because the clash set
     * is school-wide: pulling every slot back to compare them would be the whole
     * timetable, not just the offending rows.
     *
     * Per row: [0] teacherId, [1] dayOfWeek, [2] periodNumber, [3] booking count.
     */
    @Query("SELECT ts.teacher.id, ts.dayOfWeek, ts.periodNumber, COUNT(ts) FROM TimetableSlot ts " +
            "WHERE ts.teacher IS NOT NULL " +
            "GROUP BY ts.teacher.id, ts.dayOfWeek, ts.periodNumber HAVING COUNT(ts) > 1")
    List<Object[]> findTeacherClashes();

    /**
     * Where these teachers are already booked, ignoring the section being saved.
     *
     * <p>Backs the hard rule that a teacher cannot be in two rooms at once. The
     * section under edit is excluded because its rows are deleted and rewritten by
     * the same transaction — counting them would make every save conflict with the
     * week it is replacing.
     *
     * <p>Class and section are fetched so the rejection can name where the teacher
     * already is ("Krishna Bansal already teaches Class 2 A at this time") instead
     * of reporting an anonymous clash.
     */
    @Query("SELECT ts FROM TimetableSlot ts "
            + "JOIN FETCH ts.schoolClass "
            + "JOIN FETCH ts.section "
            + "WHERE ts.teacher.id IN :teacherIds AND ts.section.id <> :excludedSectionId")
    List<TimetableSlot> findBookingsForTeachersOutsideSection(@Param("teacherIds") Collection<Long> teacherIds,
                                                             @Param("excludedSectionId") Long excludedSectionId);

    /** Same shape as {@link #findTeacherClashes()} but keyed on room. */
    @Query("SELECT ts.roomNumber, ts.dayOfWeek, ts.periodNumber, COUNT(ts) FROM TimetableSlot ts " +
            "WHERE ts.roomNumber IS NOT NULL AND ts.roomNumber <> '' " +
            "GROUP BY ts.roomNumber, ts.dayOfWeek, ts.periodNumber HAVING COUNT(ts) > 1")
    List<Object[]> findRoomClashes();

    /** Periods each teacher is timetabled for, for the workload view. [0] teacherId, [1] periods. */
    @Query("SELECT ts.teacher.id, COUNT(ts) FROM TimetableSlot ts " +
            "WHERE ts.teacher IS NOT NULL GROUP BY ts.teacher.id")
    List<Object[]> countPeriodsGroupByTeacherId();

    @Query("SELECT COUNT(ts) FROM TimetableSlot ts WHERE ts.schoolClass.id = :classId")
    long countByClassId(@Param("classId") Long classId);
}
