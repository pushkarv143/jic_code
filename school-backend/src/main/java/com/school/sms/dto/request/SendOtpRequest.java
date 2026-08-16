package com.school.sms.dto.request;

import com.school.sms.entity.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpRequest {

    /**
     * An email address or a phone number. One field rather than two because the
     * caller should not have to tell the server which it typed, and because a
     * single field is what the screen actually shows.
     */
    @NotBlank(message = "Enter your email address or phone number")
    @Size(max = 150, message = "That does not look like an email address or phone number")
    private String destination;

    /**
     * What the code will be accepted for. Required rather than defaulted, so a
     * client can never obtain a login code by omitting a field.
     */
    @NotNull(message = "Purpose is required")
    private OtpPurpose purpose;
}
