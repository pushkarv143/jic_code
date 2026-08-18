package com.school.sms.repository;

import com.school.sms.entity.ClassSubjectTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClassSubjectTeacherRepository
        extends JpaRepository<ClassSubjectTeacher, Long>, JpaSpecificationExecutor<ClassSubjectTeacher> {

    boolean existsBySectionIdAndSubjectId(Long sectionId, Long subjectId);

    // Section scoping (SectionAccessGuard): does this teacher teach any subject in
    // this class/section? Both ids are matched so a section id from a different
    // class cannot be used to slip through.
    boolean existsByTeacherIdAndSchoolClassIdAndSectionId(Long teacherId, Long classId, Long sectionId);

    /** Every subject-teacher mapping of one class, for the overview and gap check. */
    List<ClassSubjectTeacher> findAllBySchoolClassIdOrderBySectionIdAscSubjectIdAsc(Long classId);

    /**
     * Workload view: per teacher, [teacherId, distinct mappings, distinct sections].
     * Aggregated in SQL so the view costs one query rather than one per teacher.
     */
    @Query("SELECT cst.teacher.id, COUNT(cst), COUNT(DISTINCT cst.section.id) " +
            "FROM ClassSubjectTeacher cst GROUP BY cst.teacher.id")
    List<Object[]> aggregateWorkloadByTeacher();
}
