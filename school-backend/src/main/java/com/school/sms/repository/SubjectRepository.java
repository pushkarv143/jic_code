package com.school.sms.repository;

import com.school.sms.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findAllBySchoolClassIdAndDeletedFalseOrderBySubjectNameAsc(Long classId);

    boolean existsBySubjectCodeIgnoreCase(String subjectCode);

    /** Subject counts for a page of classes, one row per class. [0] classId, [1] count. */
    @Query("SELECT s.schoolClass.id, COUNT(s) FROM Subject s WHERE s.deleted = false GROUP BY s.schoolClass.id")
    List<Object[]> countActiveGroupByClassId();
}
