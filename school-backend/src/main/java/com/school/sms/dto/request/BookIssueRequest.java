package com.school.sms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Exactly one of studentId/teacherId must be set — validated in
 * BookIssueServiceImpl.issue() with a BadRequestException, since this is a
 * cross-field rule bean validation can't express with the existing pattern.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookIssueRequest {

    @NotNull(message = "Book is required")
    private Long bookId;

    private Long studentId;

    private Long teacherId;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;
}
