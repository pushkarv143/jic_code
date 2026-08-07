package com.school.sms.mapper;

import com.school.sms.dto.response.ClassSubjectTeacherDto;
import com.school.sms.entity.ClassSubjectTeacher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClassSubjectTeacherMapper {

    @Mapping(target = "classId", source = "schoolClass.id")
    @Mapping(target = "className", source = "schoolClass.className")
    @Mapping(target = "sectionId", source = "section.id")
    @Mapping(target = "sectionName", source = "section.sectionName")
    @Mapping(target = "subjectId", source = "subject.id")
    @Mapping(target = "subjectName", source = "subject.subjectName")
    @Mapping(target = "teacherId", source = "teacher.id")
    @Mapping(target = "teacherName", ignore = true)
    ClassSubjectTeacherDto toDto(ClassSubjectTeacher classSubjectTeacher);
}
