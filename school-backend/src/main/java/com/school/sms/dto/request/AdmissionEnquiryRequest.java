package com.school.sms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionEnquiryRequest {

    @NotBlank(message = "Student name is required")
    private String studentName;

    @NotBlank(message = "Parent name is required")
    private String parentName;

    @NotBlank(message = "Phone is required")
    private String phone;

    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Class applying for is required")
    private String classApplying;

    private LocalDate dob;

    private String address;
}
