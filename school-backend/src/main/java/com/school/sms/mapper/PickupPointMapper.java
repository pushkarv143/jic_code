package com.school.sms.mapper;

import com.school.sms.dto.response.PickupPointDto;
import com.school.sms.entity.PickupPoint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PickupPointMapper {

    @Mapping(target = "routeId", source = "route.id")
    PickupPointDto toDto(PickupPoint pickupPoint);
}
