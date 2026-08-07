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
public class GuardianDto {

    private Long id;
    private Long studentId;
    private String name;
    private String relation;
    private String occupation;
    private String phone;
    private String email;
    private String address;
    private boolean primary;
}
