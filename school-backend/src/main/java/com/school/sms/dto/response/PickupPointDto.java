package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PickupPointDto {

    private Long id;
    private Long routeId;
    private String pointName;
    private LocalTime pickupTime;
    private LocalTime dropTime;
}
