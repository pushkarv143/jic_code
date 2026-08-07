package com.school.sms.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateStudentFeesRequest {

    @NotNull(message = "Class is required")
    private Long classId;

    @NotNull(message = "Academic year is required")
    private Long academicYearId;

    @NotEmpty(message = "At least one fee structure is required")
    private List<Long> feeStructureIds;
}
