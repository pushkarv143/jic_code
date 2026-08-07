package com.school.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusRequest {

    @NotBlank(message = "Bus number is required")
    private String busNumber;

    private Integer capacity;

    private Long driverId;

    private String vehicleModel;

    @NotBlank(message = "Registration number is required")
    private String registrationNumber;
}
