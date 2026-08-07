package com.school.sms.mapper;

import com.school.sms.dto.response.BusDto;
import com.school.sms.entity.Bus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

/** Only a toDto is generated: driverId is a raw FK resolved via DriverRepository, so
 *  BusServiceImpl builds/updates the entity by hand (same pattern as FeeStructureMapper). */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BusMapper {

    @Mapping(target = "driverId", source = "driver.id")
    @Mapping(target = "driverName", source = "driver.name")
    BusDto toDto(Bus bus);
}
