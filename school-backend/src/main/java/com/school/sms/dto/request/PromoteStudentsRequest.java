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
public class PromoteStudentsRequest {

    @NotEmpty(message = "At least one student id is required")
    private List<Long> studentIds;

    @NotNull(message = "Target class is required")
    private Long toClassId;

    @NotNull(message = "Target section is required")
    private Long toSectionId;

    @NotNull(message = "Academic year is required")
    private Long academicYearId;
}
