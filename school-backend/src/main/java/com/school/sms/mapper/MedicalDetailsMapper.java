package com.school.sms.mapper;

import com.school.sms.dto.request.MedicalDetailsRequest;
import com.school.sms.dto.response.MedicalDetailsDto;
import com.school.sms.entity.StudentMedicalDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MedicalDetailsMapper {

    @Mapping(target = "studentId", source = "student.id")
    MedicalDetailsDto toDto(StudentMedicalDetails medicalDetails);

    // id/createdAt/updatedAt are inherited from AuditableEntity and therefore not present
    // on StudentMedicalDetails's Lombok @Builder (it isn't a @SuperBuilder), so nothing to ignore here.
    @Mapping(target = "student", ignore = true)
    StudentMedicalDetails toEntity(MedicalDetailsRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(MedicalDetailsRequest request, @MappingTarget StudentMedicalDetails medicalDetails);
}
