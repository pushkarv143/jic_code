package com.school.sms.mapper;

import com.school.sms.dto.request.DesignationRequest;
import com.school.sms.dto.response.DesignationDto;
import com.school.sms.entity.Designation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DesignationMapper {

    DesignationDto toDto(Designation designation);

    // id/createdAt/updatedAt are inherited from AuditableEntity and therefore not present
    // on Designation's Lombok @Builder (it isn't a @SuperBuilder), so nothing to ignore here.
    Designation toEntity(DesignationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(DesignationRequest request, @MappingTarget Designation designation);
}
