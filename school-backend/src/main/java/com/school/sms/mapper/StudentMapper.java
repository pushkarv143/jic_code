package com.school.sms.mapper;

import com.school.sms.dto.request.StudentUpdateRequest;
import com.school.sms.dto.response.StudentDto;
import com.school.sms.entity.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StudentMapper {

    @Mapping(target = "classId", source = "schoolClass.id")
    @Mapping(target = "className", source = "schoolClass.className")
    @Mapping(target = "sectionId", source = "section.id")
    @Mapping(target = "sectionName", source = "section.sectionName")
    @Mapping(target = "academicYearId", source = "academicYear.id")
    @Mapping(target = "academicYearName", source = "academicYear.yearName")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    // Identity now comes from the student, not the optional login account, so a
    // student admitted without a login still has a name in the response. These
    // four lines previously read user.* and returned null for such students.
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "primaryGuardianName", ignore = true)
    @Mapping(target = "primaryGuardianPhone", ignore = true)
    @Mapping(target = "guardians", ignore = true)
    @Mapping(target = "medicalDetails", ignore = true)
    @Mapping(target = "documents", ignore = true)
    StudentDto toDto(Student student);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "admissionNumber", ignore = true)
    @Mapping(target = "schoolClass", ignore = true)
    @Mapping(target = "section", ignore = true)
    @Mapping(target = "photoUrl", ignore = true)
    @Mapping(target = "academicYear", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "guardians", ignore = true)
    @Mapping(target = "medicalDetails", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(StudentUpdateRequest request, @MappingTarget Student student);
}
