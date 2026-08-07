package com.school.sms.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignClassTeacherRequest {

    // Null clears the section's class teacher (unassign); non-null assigns/replaces it.
    private Long teacherId;
}
