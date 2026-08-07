package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One row per active student in an exam's class (across all its sections),
 * used to render the marks-entry grid. {@code marksObtained}/{@code gradeName}
 * are null when the student has no mark recorded yet for this schedule —
 * mirrors the same "roster with nullable status" convention as
 * {@code StudentAttendanceRowDto}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkRosterRowDto {

    private Long studentId;
    private String firstName;
    private String lastName;
    private Integer rollNumber;
    private String sectionName;
    private BigDecimal marksObtained;
    private Integer maxMarks;
    private String gradeName;
    private String remarks;
}
