package com.school.sms.dto.request;

import com.school.sms.entity.ClassOfficialRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassOfficialRequest {

    @NotNull(message = "Student is required")
    private Long studentId;

    @NotNull(message = "Role is required")
    private ClassOfficialRole role;

    /** Optional: scopes a post to one section instead of the whole class. */
    private Long sectionId;

    /** Defaults to today when omitted. */
    private LocalDate fromDate;

    private String remarks;
}
