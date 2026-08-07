package com.school.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PickupPointRequest {

    @NotBlank(message = "Point name is required")
    private String pointName;

    private LocalTime pickupTime;

    private LocalTime dropTime;
}
