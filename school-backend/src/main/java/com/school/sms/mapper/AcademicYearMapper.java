package com.school.sms.mapper;

import com.school.sms.dto.request.AcademicYearRequest;
import com.school.sms.dto.response.AcademicYearDto;
import com.school.sms.entity.AcademicYear;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AcademicYearMapper {

    AcademicYearDto toDto(AcademicYear academicYear);

    // id/createdAt/updatedAt are inherited from AuditableEntity and therefore not present
    // on AcademicYear's Lombok @Builder (it isn't a @SuperBuilder), so they are simply left
    // unmapped here rather than referenced via @Mapping(ignore = true).
    @Mapping(target = "current", ignore = true)
    AcademicYear toEntity(AcademicYearRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "current", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(AcademicYearRequest request, @MappingTarget AcademicYear academicYear);
}
