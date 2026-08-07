package com.school.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SectionRequest {

    @NotBlank(message = "Section name is required")
    private String sectionName;

    private String roomNumber;

    private Integer capacity;
}
