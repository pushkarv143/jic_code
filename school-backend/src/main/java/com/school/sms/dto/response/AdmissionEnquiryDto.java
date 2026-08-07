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
public class AdmissionEnquiryDto {

    private Long id;
    private String studentName;
    private String parentName;
    private String phone;
    private String email;
    private String classApplying;
    private LocalDate dob;
    private String address;
    private String status;
    private String documentsUrl;
    private LocalDateTime appliedAt;
}
