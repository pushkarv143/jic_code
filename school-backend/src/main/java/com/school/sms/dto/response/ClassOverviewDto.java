package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Everything the Class Overview screen shows, in one response.
 *
 * <p>Assembled server-side rather than left to the client to stitch together
 * from six endpoints: the strength, stats and warnings all derive from the same
 * roster, so fetching them separately would re-read it each time and could show
 * a strength that disagrees with the gender split beside it.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassOverviewDto {

    private Long classId;
    private String className;
    private Long academicYearId;
    private String academicYearName;

    /* ---- strength ---- */
    private int totalStudents;
    private int totalSections;
    private int totalSubjects;
    /** Sum of every section's capacity; null when no section declares one. */
    private Integer totalCapacity;
    /** totalStudents / totalCapacity as a percentage, null when capacity is unknown. */
    private Double occupancyPercentage;
    /** Active students by gender, e.g. {"MALE": 18, "FEMALE": 22}. */
    private Map<String, Long> genderSplit;

    /* ---- people ---- */
    private List<SectionDto> sections;
    private List<ClassOfficialDto> officials;
    private List<ClassSubjectTeacherDto> subjectTeachers;

    /* ---- stats ---- */
    private ClassStatsDto stats;

    /* ---- setup gaps ---- */
    private List<ClassWarningDto> warnings;
}
