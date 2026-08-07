package com.school.sms.mapper;

import com.school.sms.dto.response.FeeStructureDto;
import com.school.sms.entity.FeeStructure;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Only a toDto is generated here: the request carries raw FK ids (classId/
 * academicYearId/feeCategoryId) that must be resolved to entities via their
 * repositories first, so FeeStructureServiceImpl builds/updates the entity by
 * hand (same pattern StudentServiceImpl/SectionServiceImpl use for their own
 * FK-heavy requests).
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FeeStructureMapper {

    @Mapping(target = "classId", source = "schoolClass.id")
    @Mapping(target = "className", source = "schoolClass.className")
    @Mapping(target = "academicYearId", source = "academicYear.id")
    @Mapping(target = "academicYearName", source = "academicYear.yearName")
    @Mapping(target = "feeCategoryId", source = "feeCategory.id")
    @Mapping(target = "feeCategoryName", source = "feeCategory.name")
    FeeStructureDto toDto(FeeStructure feeStructure);
}
