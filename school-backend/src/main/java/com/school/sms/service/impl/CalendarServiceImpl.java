package com.school.sms.service.impl;

import com.school.sms.dto.response.BirthdayCalendarDto;
import com.school.sms.dto.response.StudentBirthdayDto;
import com.school.sms.dto.response.TeacherBirthdayDto;
import com.school.sms.exception.BadRequestException;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.service.CalendarService;
import com.school.sms.util.NameUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    @Override
    @Transactional(readOnly = true)
    public BirthdayCalendarDto getBirthdays(int month) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12");
        }

        var students = studentRepository.findAllByBirthMonth(month).stream()
                .map(student -> StudentBirthdayDto.builder()
                        .id(student.getId())
                        .name(student.getUser() != null
                                ? NameUtil.fullName(student.getUser().getFirstName(), student.getUser().getLastName())
                                : null)
                        .dateOfBirth(student.getDateOfBirth())
                        .className(student.getSchoolClass() != null ? student.getSchoolClass().getClassName() : null)
                        .sectionName(student.getSection() != null ? student.getSection().getSectionName() : null)
                        .build())
                .toList();

        var teachers = teacherRepository.findAllByBirthMonth(month).stream()
                .map(teacher -> TeacherBirthdayDto.builder()
                        .id(teacher.getId())
                        .name(teacher.getUser() != null
                                ? NameUtil.fullName(teacher.getUser().getFirstName(), teacher.getUser().getLastName())
                                : null)
                        .dateOfBirth(teacher.getDateOfBirth())
                        .departmentName(teacher.getDepartment() != null ? teacher.getDepartment().getName() : null)
                        .build())
                .toList();

        return BirthdayCalendarDto.builder()
                .students(students)
                .teachers(teachers)
                .build();
    }
}
