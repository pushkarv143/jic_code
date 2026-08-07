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
public class StudentTransportDto {

    private Long id;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private Long routeId;
    private String routeName;
    private Long pickupPointId;
    private String pickupPointName;
    private BigDecimal monthlyFee;
}
