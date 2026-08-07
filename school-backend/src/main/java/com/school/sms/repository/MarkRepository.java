package com.school.sms.repository;

import com.school.sms.entity.Mark;
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
}
