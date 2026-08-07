package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostelVisitorDto {

    private Long id;
    private Long studentId;
    private String studentName;
    private String visitorName;
    private String relation;
    private String phone;
    private LocalDate visitDate;
    private String purpose;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
}
