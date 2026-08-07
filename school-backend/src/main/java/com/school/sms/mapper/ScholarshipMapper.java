package com.school.sms.mapper;

import com.school.sms.dto.response.ScholarshipDto;
import com.school.sms.entity.Scholarship;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Only a toDto is generated: ScholarshipServiceImpl resolves student/academic
 * year/approvedBy by hand, same rationale as FeeStructureMapper.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ScholarshipMapper {

    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentName", ignore = true)
    @Mapping(target = "admissionNumber", source = "student.admissionNumber")
    @Mapping(target = "academicYearId", source = "academicYear.id")
    @Mapping(target = "academicYearName", source = "academicYear.yearName")
    @Mapping(target = "approvedByName", ignore = true)
    ScholarshipDto toDto(Scholarship scholarship);
}
