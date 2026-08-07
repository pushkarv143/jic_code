package com.school.sms.mapper;

import com.school.sms.dto.response.BookDto;
import com.school.sms.entity.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Only a toDto is generated: the request carries a raw categoryId that must
 * be resolved via BookCategoryRepository first, and availableCopies is
 * computed by the service (not taken verbatim from the request) — so
 * BookServiceImpl builds/updates the entity by hand, same pattern as
 * FeeStructureMapper/FeeStructureServiceImpl.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BookMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    BookDto toDto(Book book);
}
