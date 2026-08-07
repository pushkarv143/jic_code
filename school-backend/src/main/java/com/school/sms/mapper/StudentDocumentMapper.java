package com.school.sms.mapper;

import com.school.sms.dto.response.StudentDocumentDto;
import com.school.sms.entity.StudentDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentDocumentMapper {

    @Mapping(target = "studentId", source = "student.id")
    StudentDocumentDto toDto(StudentDocument studentDocument);
}
