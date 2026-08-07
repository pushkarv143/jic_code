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
public class LibrarySummaryReportDto {

    private long totalBooks;
    private long totalIssued;
    private long totalOverdue;
    private List<CategoryCountDto> byCategory;
}
