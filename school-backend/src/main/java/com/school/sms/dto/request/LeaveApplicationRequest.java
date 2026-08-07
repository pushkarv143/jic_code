package com.school.sms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApplicationRequest {

    @Size(max = 50, message = "Leave type must not exceed 50 characters")
    private String leaveType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    // Cross-field check (endDate >= startDate) is done in the service layer,
    // per the module's convention (bean validation alone can't express it).
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;
}
