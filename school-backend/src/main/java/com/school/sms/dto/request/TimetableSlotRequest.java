package com.school.sms.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** One period of a {@link SaveTimetableRequest} batch. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimetableSlotRequest {

    @NotNull(message = "Day is required")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Period number is required")
    @Min(value = 1, message = "Period number starts at 1")
    private Integer periodNumber;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    /** Null for a non-teaching period; pair with {@code label}. */
    private Long subjectId;

    private Long teacherId;

    private String roomNumber;

    private String label;
}
