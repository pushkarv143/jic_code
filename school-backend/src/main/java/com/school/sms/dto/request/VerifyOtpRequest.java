package com.school.sms.dto.request;

import com.school.sms.entity.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {

    @NotBlank(message = "Enter your email address or phone number")
    private String destination;

    @NotNull(message = "Purpose is required")
    private OtpPurpose purpose;

    /**
     * Exactly six digits. Enforced here so a malformed code is rejected before it
     * costs the account one of its few verification attempts.
     */
    @NotBlank(message = "Enter the 6-digit code")
    @Pattern(regexp = "^[0-9]{6}$", message = "The code is 6 digits")
    private String code;
}
