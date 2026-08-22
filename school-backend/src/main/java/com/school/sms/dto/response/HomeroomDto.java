package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The section a caller is homeroom teacher of.
 *
 * <p>Null on {@link MyAccessDto} for everyone who is homeroom of nothing, which is
 * most logins — including administrators. The frontend treats null as "hide the My
 * Class module", so this field is the single signal that decides whether that
 * module exists for a user, in place of the {@code CLASS_TEACHER} role that only
 * 17 of its 44 holders can actually back up with an assignment.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeroomDto {

    private Long sectionId;
    private String sectionName;
    private Long classId;
    private String className;
    private Long academicYearId;
    private String academicYear;
    /** Students currently on the roster, so the UI can label the module without a second call. */
    private long studentCount;
}
