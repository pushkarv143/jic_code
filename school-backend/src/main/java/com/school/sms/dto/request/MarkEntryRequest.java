package com.school.sms.dto.request;

import jakarta.validation.Valid;
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
public class MarkEntryRequest {

    @NotNull(message = "Exam schedule is required")
    private Long examScheduleId;

    @NotEmpty(message = "At least one mark record is required")
    @Valid
    private List<MarkRecordItem> records;
}
