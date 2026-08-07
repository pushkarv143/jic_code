package com.school.sms.mapper;

import com.school.sms.dto.request.BookCategoryRequest;
import com.school.sms.dto.response.BookCategoryDto;
import com.school.sms.entity.BookCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BookCategoryMapper {

    BookCategoryDto toDto(BookCategory category);

    BookCategory toEntity(BookCategoryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(BookCategoryRequest request, @MappingTarget BookCategory category);
}
