package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** One appointment. {@code current} is true while {@code toDate} is null. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassOfficialDto {

    private Long id;
    private Long classId;
    private String className;
    private Long sectionId;
    private String sectionName;
    private Long studentId;
    private String studentName;
    private Integer rollNumber;
    private String admissionNumber;
    private String role;
    private LocalDate fromDate;
    private LocalDate toDate;
    private boolean current;
    private String remarks;
}
