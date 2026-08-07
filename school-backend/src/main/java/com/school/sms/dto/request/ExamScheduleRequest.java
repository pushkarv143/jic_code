package com.school.sms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExamScheduleRequest {

    @NotNull(message = "Subject is required")
    private Long subjectId;

    private LocalDate examDate;

    private LocalTime startTime;

    private LocalTime endTime;

    @NotNull(message = "Max marks is required")
    @Positive(message = "Max marks must be positive")
    private Integer maxMarks;

    private String roomNumber;
}
