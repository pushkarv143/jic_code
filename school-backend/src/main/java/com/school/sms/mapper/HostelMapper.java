package com.school.sms.mapper;

import com.school.sms.dto.request.HostelRequest;
import com.school.sms.dto.response.HostelDto;
import com.school.sms.entity.Hostel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface HostelMapper {

    HostelDto toDto(Hostel hostel);

    Hostel toEntity(HostelRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(HostelRequest request, @MappingTarget Hostel hostel);
}
