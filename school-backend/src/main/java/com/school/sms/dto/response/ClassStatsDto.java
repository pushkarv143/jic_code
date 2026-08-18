package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cross-module numbers for one class over a date range. Every field is nullable:
 * a class with no marked attendance, no fee structure or no graded exam has no
 * figure to report, and a null says that plainly where a 0 would read as "bad".
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassStatsDto {

    /** PRESENT + half credit for HALF_DAY, over all marked days in range. */
    private Double attendancePercentage;
    private Long attendanceMarkedDays;

    /** Students with at least one UNPAID/PARTIAL/OVERDUE fee row. */
    private Long feeDefaulterCount;
    private Double feeOutstandingAmount;

    /** Mean of marks_obtained / max_marks across every graded schedule for the class. */
    private Double averageMarksPercentage;
    private Long gradedStudentCount;
}
