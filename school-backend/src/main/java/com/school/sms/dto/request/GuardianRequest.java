package com.school.sms.dto.request;

import com.school.sms.util.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuardianRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String relation;

    private String occupation;

    @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
    private String phone;

    @Email(message = "Email must be valid")
    private String email;

    private String address;

    private boolean primary;
}
