package com.school.sms.dto.request;

import com.school.sms.entity.ClassOfficialRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Appointing a class post within the caller's own section.
 *
 * <p>Like {@link MyClassStudentUpdateRequest}, this omits the class and section
 * ids that {@link ClassOfficialRequest} carries — both are taken from the caller's
 * homeroom assignment.
 *
 * <p>The student still has to belong to that class, but this type is not what
 * enforces it: {@code ClassOfficialServiceImpl.appoint} already refuses to make a
 * student of one class the head boy of another, so a class teacher naming a
 * stranger fails there rather than needing a second check here.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyClassOfficialRequest {

    @NotNull(message = "Student is required")
    private Long studentId;

    @NotNull(message = "Post is required")
    private ClassOfficialRole role;

    /** Defaults to today when omitted. */
    private LocalDate fromDate;

    private String remarks;
}
