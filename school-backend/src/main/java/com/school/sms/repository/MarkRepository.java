package com.school.sms.repository;

import com.school.sms.entity.Mark;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface MarkRepository extends JpaRepository<Mark, Long>, JpaSpecificationExecutor<Mark> {

    Optional<Mark> findByExamScheduleIdAndStudentId(Long examScheduleId, Long studentId);

    // Marks-entry roster: every mark already recorded for one exam schedule.
    List<Mark> findAllByExamScheduleId(Long examScheduleId);

    // Report card: every mark for a student across all schedules of one exam.
    List<Mark> findAllByStudentIdAndExamScheduleExamId(Long studentId, Long examId);

    // Class/section results: every mark recorded for all schedules of one exam.
    List<Mark> findAllByExamScheduleExamId(Long examId);

    /**
     * Class-overview marks panel: [mean percentage, distinct students graded]
     * across every exam of one class.
     *
     * <p>Averages the per-mark percentage rather than dividing summed marks by
     * summed maximums, so a 10-mark quiz and a 100-mark paper weigh equally —
     * the alternative lets one big paper drown out everything else. Schedules
     * with a null or zero maxMarks are excluded rather than treated as 0, which
     * would divide by zero.
     */
    @Query("SELECT AVG(m.marksObtained * 100.0 / m.examSchedule.maxMarks), COUNT(DISTINCT m.student.id) " +
            "FROM Mark m WHERE m.examSchedule.exam.schoolClass.id = :classId " +
            "AND m.examSchedule.maxMarks IS NOT NULL AND m.examSchedule.maxMarks > 0")
    List<Object[]> aggregateAverageForClass(@Param("classId") Long classId);
}
