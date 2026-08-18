package com.school.sms.repository;

import com.school.sms.entity.StudentAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentAttendanceRepository
        extends JpaRepository<StudentAttendance, Long>, JpaSpecificationExecutor<StudentAttendance> {

    Optional<StudentAttendance> findByStudentIdAndAttendanceDate(Long studentId, LocalDate attendanceDate);

    List<StudentAttendance> findAllByStudentIdInAndAttendanceDate(List<Long> studentIds, LocalDate attendanceDate);

    List<StudentAttendance> findAllByStudentIdInAndAttendanceDateBetween(
            List<Long> studentIds, LocalDate startDate, LocalDate endDate);

    List<StudentAttendance> findAllByStudentIdAndAttendanceDateBetween(
            Long studentId, LocalDate startDate, LocalDate endDate);

    /**
     * School-wide (or single-class, when classId is given) attendance aggregate
     * for /api/v1/reports/attendance-summary. Per class-name row:
     * [0] = className, [1] = present-equivalent count (HALF_DAY counts as 0.5,
     * PRESENT as 1, everything else 0), [2] = total marked records.
     */
    @Query("SELECT sa.schoolClass.className, " +
            "SUM(CASE WHEN sa.status = 'PRESENT' THEN 1.0 WHEN sa.status = 'HALF_DAY' THEN 0.5 ELSE 0.0 END), " +
            "COUNT(sa) FROM StudentAttendance sa " +
            "WHERE sa.attendanceDate BETWEEN :startDate AND :endDate " +
            "AND (:classId IS NULL OR sa.schoolClass.id = :classId) " +
            "GROUP BY sa.schoolClass.className ORDER BY sa.schoolClass.className")
    List<Object[]> aggregateAttendanceByClass(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("classId") Long classId);

    /**
     * Class-overview attendance panel: a single row of [present-equivalent,
     * total marked] for one class over a range.
     *
     * <p>Scores PRESENT as 1 and HALF_DAY as 0.5, matching
     * {@link #aggregateAttendanceByClass} and StudentAttendanceServiceImpl's
     * per-student summary. Note LATE scores 0 in all three, which disagrees with
     * sp_calculate_student_attendance_percentage in 05_procedures.sql — that
     * procedure counts LATE as a full day but is called by nothing, so the
     * application's own definition is the one that holds.
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN sa.status = 'PRESENT' THEN 1.0 WHEN sa.status = 'HALF_DAY' THEN 0.5 ELSE 0.0 END), 0), " +
            "COUNT(sa) FROM StudentAttendance sa " +
            "WHERE sa.schoolClass.id = :classId AND sa.attendanceDate BETWEEN :startDate AND :endDate")
    List<Object[]> aggregateForClassBetween(@Param("classId") Long classId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);
}
