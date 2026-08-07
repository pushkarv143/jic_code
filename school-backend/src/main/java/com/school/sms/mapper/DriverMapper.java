package com.school.sms.mapper;

import com.school.sms.dto.request.DriverRequest;
import com.school.sms.dto.response.DriverDto;
import com.school.sms.entity.Driver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DriverMapper {

    DriverDto toDto(Driver driver);

    Driver toEntity(DriverRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(DriverRequest request, @MappingTarget Driver driver);
}
