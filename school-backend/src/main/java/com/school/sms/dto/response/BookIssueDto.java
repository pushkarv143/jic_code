package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookIssueDto {

    private Long id;
    private Long bookId;
    private String bookTitle;
    private Long studentId;
    private String studentName;
    private Long teacherId;
    private String teacherName;
    /** Whichever of student/teacher is set on this issue ("STUDENT" or "TEACHER"). */
    private String borrowerType;
    private String borrowerName;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private BigDecimal fineAmount;
    private String status;
}
