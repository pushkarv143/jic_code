package com.school.sms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentTransportRequest {

    @NotNull(message = "Student is required")
    private Long studentId;

    @NotNull(message = "Route is required")
    private Long routeId;

    @NotNull(message = "Pickup point is required")
    private Long pickupPointId;

    private BigDecimal monthlyFee;
}
