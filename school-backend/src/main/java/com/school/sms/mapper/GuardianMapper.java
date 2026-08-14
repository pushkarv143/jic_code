package com.school.sms.mapper;

import com.school.sms.dto.request.GuardianRequest;
import com.school.sms.dto.response.GuardianDto;
import com.school.sms.entity.Guardian;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface GuardianMapper {

    // The entity field is `primary` and the DTO field is `isPrimary`, so MapStruct
    // cannot pair them by name. The DTO was renamed to pin the JSON property to
    // "isPrimary", which is what both clients and GuardianRequest already use.
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "isPrimary", source = "primary")
    GuardianDto toDto(Guardian guardian);

    // id/createdAt/updatedAt are inherited from AuditableEntity and therefore not present
    // on Guardian's Lombok @Builder (it isn't a @SuperBuilder), so nothing to ignore here.
    @Mapping(target = "student", ignore = true)
    Guardian toEntity(GuardianRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(GuardianRequest request, @MappingTarget Guardian guardian);
}
