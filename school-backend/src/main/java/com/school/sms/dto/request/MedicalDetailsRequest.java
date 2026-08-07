package com.school.sms.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedicalDetailsRequest {

    private BigDecimal heightCm;

    private BigDecimal weightKg;

    private String allergies;

    private String medicalConditions;

    private String doctorName;

    private String doctorContact;
}
