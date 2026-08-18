package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A setup gap worth showing an administrator — a subject nobody teaches, a
 * section over capacity, a room booked twice.
 *
 * <p>Deliberately data rather than prose: {@code code} lets the client group and
 * translate, {@code message} is the fallback, and the ids let it link straight
 * to the thing that needs fixing.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassWarningDto {

    /** e.g. SUBJECT_WITHOUT_TEACHER, SECTION_OVER_CAPACITY, SECTION_WITHOUT_CLASS_TEACHER. */
    private String code;
    /** INFO | WARNING — nothing here blocks anything, so there is no ERROR. */
    private String severity;
    private String message;
    private Long sectionId;
    private String sectionName;
    private Long subjectId;
    private String subjectName;
}
