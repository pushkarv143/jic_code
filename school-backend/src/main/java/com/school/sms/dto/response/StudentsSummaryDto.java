package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentsSummaryDto {

    private long totalActive;
    private List<ClassCountDto> byClass;
    private List<StatusCountDto> byStatus;
}
