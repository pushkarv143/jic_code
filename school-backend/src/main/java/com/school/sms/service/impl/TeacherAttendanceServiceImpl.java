package com.school.sms.service.impl;

import com.school.sms.dto.request.MarkTeacherAttendanceRequest;
import com.school.sms.dto.request.TeacherAttendanceRecordItem;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.TeacherAttendanceRecordDto;
import com.school.sms.dto.response.TeacherAttendanceRowDto;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.TeacherAttendance;
import com.school.sms.entity.TeacherStatus;
import com.school.sms.entity.User;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.TeacherAttendanceRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.service.TeacherAttendanceService;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TeacherAttendanceServiceImpl implements TeacherAttendanceService {

    private final TeacherAttendanceRepository teacherAttendanceRepository;
    private final TeacherRepository teacherRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TeacherAttendanceRowDto> getGrid(LocalDate date) {
        List<Teacher> teachers = teacherRepository.findAllByDeletedFalseAndStatusOrderByIdAsc(TeacherStatus.ACTIVE);

        Map<Long, TeacherAttendance> byTeacherId = new HashMap<>();
        if (!teachers.isEmpty()) {
            List<Long> teacherIds = teachers.stream().map(Teacher::getId).toList();
            teacherAttendanceRepository.findAllByTeacherIdInAndAttendanceDate(teacherIds, date)
                    .forEach(record -> byTeacherId.put(record.getTeacher().getId(), record));
        }

        return teachers.stream()
                .map(teacher -> {
                    TeacherAttendance record = byTeacherId.get(teacher.getId());
                    User user = teacher.getUser();
                    return TeacherAttendanceRowDto.builder()
                            .teacherId(teacher.getId())
                            .firstName(user != null ? user.getFirstName() : null)
                            .lastName(user != null ? user.getLastName() : null)
                            .employeeId(teacher.getEmployeeId())
                            .status(record != null ? record.getStatus().name() : null)
                            .checkIn(record != null ? record.getCheckIn() : null)
                            .checkOut(record != null ? record.getCheckOut() : null)
                            .remarks(record != null ? record.getRemarks() : null)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public int mark(MarkTeacherAttendanceRequest request) {
        int count = 0;
        for (TeacherAttendanceRecordItem item : request.getRecords()) {
            Teacher teacher = teacherRepository.findById(item.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", item.getTeacherId()));

            TeacherAttendance record = teacherAttendanceRepository
                    .findByTeacherIdAndAttendanceDate(teacher.getId(), request.getAttendanceDate())
                    .orElseGet(() -> TeacherAttendance.builder()
                            .teacher(teacher)
                            .attendanceDate(request.getAttendanceDate())
                            .build());

            record.setStatus(item.getStatus());
            record.setCheckIn(item.getCheckIn());
            record.setCheckOut(item.getCheckOut());
            record.setRemarks(item.getRemarks());

            teacherAttendanceRepository.save(record);
            count++;
        }
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TeacherAttendanceRecordDto> getReport(Long teacherId, LocalDate startDate, LocalDate endDate,
                                                               Pageable pageable) {
        Specification<TeacherAttendance> spec = new SpecificationBuilder<TeacherAttendance>()
                .with(teacherId != null, "teacher.id", SearchOperation.EQUALS, teacherId)
                .with(startDate != null, "attendanceDate", SearchOperation.GREATER_THAN_EQUAL, startDate)
                .with(endDate != null, "attendanceDate", SearchOperation.LESS_THAN_EQUAL, endDate)
                .build();

        Page<TeacherAttendance> page = teacherAttendanceRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(this::toDto));
    }

    private TeacherAttendanceRecordDto toDto(TeacherAttendance record) {
        Teacher teacher = record.getTeacher();
        User user = teacher.getUser();
        return TeacherAttendanceRecordDto.builder()
                .id(record.getId())
                .teacherId(teacher.getId())
                .firstName(user != null ? user.getFirstName() : null)
                .lastName(user != null ? user.getLastName() : null)
                .employeeId(teacher.getEmployeeId())
                .attendanceDate(record.getAttendanceDate())
                .status(record.getStatus().name())
                .checkIn(record.getCheckIn())
                .checkOut(record.getCheckOut())
                .remarks(record.getRemarks())
                .build();
    }
}
