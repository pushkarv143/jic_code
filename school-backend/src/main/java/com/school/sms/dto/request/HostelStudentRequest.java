package com.school.sms.dto.request;

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
public class HostelStudentRequest {

    @NotNull(message = "Student is required")
    private Long studentId;

    @NotNull(message = "Room is required")
    private Long roomId;

    /** Defaults to today (in the service) when omitted. */
    private LocalDate allocationDate;
}
