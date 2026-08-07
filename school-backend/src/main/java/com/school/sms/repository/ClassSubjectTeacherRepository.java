package com.school.sms.repository;

import com.school.sms.entity.ClassSubjectTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClassSubjectTeacherRepository
        extends JpaRepository<ClassSubjectTeacher, Long>, JpaSpecificationExecutor<ClassSubjectTeacher> {

    boolean existsBySectionIdAndSubjectId(Long sectionId, Long subjectId);
}
