package com.school.sms.mapper;

import com.school.sms.dto.request.ExamTypeRequest;
import com.school.sms.dto.response.ExamTypeDto;
import com.school.sms.entity.ExamType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ExamTypeMapper {

    ExamTypeDto toDto(ExamType examType);

    ExamType toEntity(ExamTypeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ExamTypeRequest request, @MappingTarget ExamType examType);
}
