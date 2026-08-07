package com.school.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Bound via @ModelAttribute from a multipart/form-data body (POST/PUT
 * /api/v1/assignments) — the accompanying "file" part is bound separately
 * via @RequestPart, same split used by StudentController#addDocument.
 */
@Getter
@Setter
public class AssignmentFormRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Class is required")
    private Long classId;

    @NotNull(message = "Section is required")
    private Long sectionId;

    @NotNull(message = "Subject is required")
    private Long subjectId;

    // Explicit teacherId is only honored for admin roles creating on behalf of
    // a teacher; TEACHER/CLASS_TEACHER callers are always pinned to their own
    // teacher record regardless of what (if anything) is submitted here.
    private Long teacherId;

    @NotNull(message = "Assigned date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate assignedDate;

    @NotNull(message = "Due date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;
}
