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
public class ExamRequest {

    @NotNull(message = "Exam type is required")
    private Long examTypeId;

    @NotNull(message = "Class is required")
    private Long classId;

    @NotNull(message = "Academic year is required")
    private Long academicYearId;

    private LocalDate startDate;

    private LocalDate endDate;
}
