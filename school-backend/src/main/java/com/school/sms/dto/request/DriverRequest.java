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
public class DriverRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String phone;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    private String address;
}
