package com.school.sms.mapper;

import com.school.sms.dto.request.SubjectRequest;
import com.school.sms.dto.response.SubjectDto;
import com.school.sms.entity.Subject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SubjectMapper {

    @Mapping(target = "classId", source = "schoolClass.id")
    @Mapping(target = "className", source = "schoolClass.className")
    SubjectDto toDto(Subject subject);

    // id/createdAt/updatedAt are inherited from AuditableEntity and therefore not present
    // on Subject's Lombok @Builder (it isn't a @SuperBuilder), so nothing to ignore here.
    @Mapping(target = "schoolClass", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Subject toEntity(SubjectRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "schoolClass", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(SubjectRequest request, @MappingTarget Subject subject);
}
