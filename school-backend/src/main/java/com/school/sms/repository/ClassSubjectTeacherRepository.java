package com.school.sms.repository;

import com.school.sms.entity.ClassSubjectTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClassSubjectTeacherRepository
        extends JpaRepository<ClassSubjectTeacher, Long>, JpaSpecificationExecutor<ClassSubjectTeacher> {

    boolean existsBySectionIdAndSubjectId(Long sectionId, Long subjectId);

    // Section scoping (SectionAccessGuard): does this teacher teach any subject in
    // this class/section? Both ids are matched so a section id from a different
    // class cannot be used to slip through.
    boolean existsByTeacherIdAndSchoolClassIdAndSectionId(Long teacherId, Long classId, Long sectionId);
}
