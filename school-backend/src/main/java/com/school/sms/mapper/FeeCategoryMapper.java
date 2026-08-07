package com.school.sms.mapper;

import com.school.sms.dto.request.FeeCategoryRequest;
import com.school.sms.dto.response.FeeCategoryDto;
import com.school.sms.entity.FeeCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FeeCategoryMapper {

    FeeCategoryDto toDto(FeeCategory feeCategory);

    FeeCategory toEntity(FeeCategoryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(FeeCategoryRequest request, @MappingTarget FeeCategory feeCategory);
}
