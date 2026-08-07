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
}
