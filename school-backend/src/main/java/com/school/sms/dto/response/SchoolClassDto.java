package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolClassDto {

    private Long id;
    private String className;
    private Long academicYearId;
    private String academicYearName;
    private int sectionCount;
    private int subjectCount;
    /** Active students across every section of this class. */
    private Integer studentCount;

    /*
     * Sections and current post-holders ride along with the list so the class
     * screen can offer a class-teacher dropdown per section and a dropdown per
     * class post without a follow-up request per row. Both are batch-loaded, so
     * a page costs a fixed number of queries however many classes it holds.
     *
     * Null rather than empty on the single-class endpoints, which do not need
     * them — an empty list there would read as "this class has no sections".
     */
    private List<SectionDto> sections;
    private List<ClassOfficialDto> officials;
}
