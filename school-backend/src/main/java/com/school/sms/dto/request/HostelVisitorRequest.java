package com.school.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HostelVisitorRequest {

    @NotNull(message = "Student is required")
    private Long studentId;

    @NotBlank(message = "Visitor name is required")
    private String visitorName;

    private String relation;

    private String phone;

    /** Defaults to today (in the service) when omitted. */
    private LocalDate visitDate;

    private String purpose;
}
