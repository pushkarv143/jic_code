package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostelStudentDto {

    private Long id;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private Long roomId;
    private String roomNumber;
    private Long hostelId;
    private String hostelName;
    private LocalDate allocationDate;
    private LocalDate vacateDate;
    private String status;
}
