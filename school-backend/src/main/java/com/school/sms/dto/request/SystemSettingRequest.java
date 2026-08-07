package com.school.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingRequest {

    @NotBlank(message = "Key is required")
    @Size(max = 100, message = "Key must not exceed 100 characters")
    private String key;

    @Size(max = 500, message = "Value must not exceed 500 characters")
    private String value;
}
