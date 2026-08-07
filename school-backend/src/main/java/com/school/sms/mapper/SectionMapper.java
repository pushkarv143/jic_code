package com.school.sms.mapper;

import com.school.sms.dto.request.SectionRequest;
import com.school.sms.dto.response.SectionDto;
import com.school.sms.entity.Section;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SectionMapper {

    @Mapping(target = "classId", source = "schoolClass.id")
    @Mapping(target = "className", source = "schoolClass.className")
    @Mapping(target = "classTeacherId", source = "classTeacher.id")
    @Mapping(target = "classTeacherName", ignore = true)
    SectionDto toDto(Section section);

    // id/createdAt/updatedAt are inherited from AuditableEntity and therefore not present
    // on Section's Lombok @Builder (it isn't a @SuperBuilder), so nothing to ignore here.
    @Mapping(target = "schoolClass", ignore = true)
    @Mapping(target = "classTeacher", ignore = true)
    Section toEntity(SectionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "schoolClass", ignore = true)
    @Mapping(target = "classTeacher", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(SectionRequest request, @MappingTarget Section section);
}
