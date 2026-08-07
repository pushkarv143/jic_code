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
public class ChildDto {

    private Long studentId;
    private String firstName;
    private String lastName;
    private String admissionNumber;
    private String className;
    private String sectionName;
    private String photoUrl;
}
