package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyMaterialDto {

    private Long id;
    private Long classId;
    private String className;
    /** Null means the material is shared with every section of the class. */
    private Long sectionId;
    private String sectionName;
    private Long subjectId;
    private String subjectName;
    private Long teacherId;
    private String teacherName;

    private String title;
    private String description;
    private String materialType;
    private String fileUrl;
    private String externalUrl;
    private boolean published;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Whether the caller may edit or delete this material — true for management, and
     * for the teacher who uploaded it. Sent so the clients can render the right
     * controls without re-deriving the ownership rule, which the server owns.
     */
    private boolean canManage;
}
