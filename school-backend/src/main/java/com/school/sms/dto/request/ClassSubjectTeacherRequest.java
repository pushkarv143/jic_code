package com.school.sms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassSubjectTeacherRequest {

    @NotNull(message = "Class id is required")
    private Long classId;

    @NotNull(message = "Section id is required")
    private Long sectionId;

    @NotNull(message = "Subject id is required")
    private Long subjectId;

    @NotNull(message = "Teacher id is required")
    private Long teacherId;
}
