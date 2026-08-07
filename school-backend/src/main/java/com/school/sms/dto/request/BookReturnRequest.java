package com.school.sms.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookReturnRequest {

    /** Defaults to today (in the service) when omitted. */
    private LocalDate returnDate;
}
