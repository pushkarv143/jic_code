package com.school.sms.repository;

import com.school.sms.entity.TeacherAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeacherAttendanceRepository
        extends JpaRepository<TeacherAttendance, Long>, JpaSpecificationExecutor<TeacherAttendance> {

    Optional<TeacherAttendance> findByTeacherIdAndAttendanceDate(Long teacherId, LocalDate attendanceDate);

    List<TeacherAttendance> findAllByTeacherIdInAndAttendanceDate(List<Long> teacherIds, LocalDate attendanceDate);
}
