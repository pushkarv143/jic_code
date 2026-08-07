package com.school.sms.mapper;

import com.school.sms.dto.response.HostelRoomDto;
import com.school.sms.entity.HostelRoom;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface HostelRoomMapper {

    @Mapping(target = "hostelId", source = "hostel.id")
    @Mapping(target = "hostelName", source = "hostel.name")
    HostelRoomDto toDto(HostelRoom room);
}
