package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalDetailsDto {

    private Long id;
    private Long studentId;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private String allergies;
    private String medicalConditions;
    private String doctorName;
    private String doctorContact;
}
