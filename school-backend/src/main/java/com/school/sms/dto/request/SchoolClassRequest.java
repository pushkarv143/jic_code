package com.school.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClassRequest {

    @NotBlank(message = "Class name is required")
    private String className;

    @NotNull(message = "Academic year is required")
    private Long academicYearId;
}
