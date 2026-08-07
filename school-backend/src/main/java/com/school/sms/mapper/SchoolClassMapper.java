package com.school.sms.mapper;

import com.school.sms.dto.request.SchoolClassRequest;
import com.school.sms.dto.response.SchoolClassDto;
import com.school.sms.entity.SchoolClass;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SchoolClassMapper {

    @Mapping(target = "academicYearId", source = "academicYear.id")
    @Mapping(target = "academicYearName", source = "academicYear.yearName")
    @Mapping(target = "sectionCount", ignore = true)
    @Mapping(target = "subjectCount", ignore = true)
    SchoolClassDto toDto(SchoolClass schoolClass);

    // id/createdAt/updatedAt are inherited from AuditableEntity and therefore not present
    // on SchoolClass's Lombok @Builder (it isn't a @SuperBuilder), so nothing to ignore here.
    @Mapping(target = "academicYear", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    SchoolClass toEntity(SchoolClassRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "academicYear", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(SchoolClassRequest request, @MappingTarget SchoolClass schoolClass);
}
