package com.school.sms.mapper;

import com.school.sms.dto.response.FeePaymentDto;
import com.school.sms.entity.FeePayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * studentId/studentName/collectedByName need a join beyond this entity's own
 * fields, so FeePaymentServiceImpl fills those in after calling toDto (same
 * "map the flat columns, enrich the rest in the service" split used by
 * StudentServiceImpl.toListDto/enrichPrimaryGuardian).
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FeePaymentMapper {

    @Mapping(target = "studentFeeId", source = "studentFee.id")
    @Mapping(target = "studentId", ignore = true)
    @Mapping(target = "studentName", ignore = true)
    @Mapping(target = "collectedByName", ignore = true)
    FeePaymentDto toDto(FeePayment feePayment);
}
