package com.school.sms.mapper;

import com.school.sms.dto.response.RouteDto;
import com.school.sms.entity.Route;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RouteMapper {

    @Mapping(target = "busId", source = "bus.id")
    @Mapping(target = "busNumber", source = "bus.busNumber")
    RouteDto toDto(Route route);
}
