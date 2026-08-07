package com.school.sms.mapper;

import com.school.sms.dto.request.DepartmentRequest;
import com.school.sms.dto.response.DepartmentDto;
import com.school.sms.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DepartmentMapper {

    DepartmentDto toDto(Department department);

    // id/createdAt/updatedAt are inherited from AuditableEntity and therefore not present
    // on Department's Lombok @Builder (it isn't a @SuperBuilder), so nothing to ignore here.
    Department toEntity(DepartmentRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(DepartmentRequest request, @MappingTarget Department department);
}
