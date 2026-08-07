package com.school.sms.repository;

import com.school.sms.entity.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {

    List<ExamSchedule> findAllByExamIdOrderByExamDateAsc(Long examId);

    boolean existsByExamIdAndSubjectId(Long examId, Long subjectId);

    boolean existsByExamIdAndSubjectIdAndIdNot(Long examId, Long subjectId, Long id);

    // Simple overlap check per the round brief: same exam_date + room_number
    // can't host two schedules of the same exam.
    boolean existsByExamIdAndExamDateAndRoomNumber(Long examId, LocalDate examDate, String roomNumber);

    boolean existsByExamIdAndExamDateAndRoomNumberAndIdNot(Long examId, LocalDate examDate, String roomNumber, Long id);
}
