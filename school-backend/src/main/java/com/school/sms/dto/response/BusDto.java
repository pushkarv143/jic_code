package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusDto {

    private Long id;
    private String busNumber;
    private Integer capacity;
    private Long driverId;
    private String driverName;
    private String vehicleModel;
    private String registrationNumber;
}
