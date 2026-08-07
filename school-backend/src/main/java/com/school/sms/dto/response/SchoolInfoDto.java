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
public class SchoolInfoDto {

    private Long id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String logoUrl;
    private Integer establishedYear;
    private String affiliationNumber;
}
