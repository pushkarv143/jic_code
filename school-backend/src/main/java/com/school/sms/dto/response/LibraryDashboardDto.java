package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryDashboardDto {

    private long totalBooks;
    private long totalCopies;
    private long availableCopies;
    private long issuedCount;
    private long overdueCount;
}
