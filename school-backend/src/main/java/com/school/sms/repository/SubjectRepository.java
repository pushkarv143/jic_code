package com.school.sms.repository;

import com.school.sms.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findAllBySchoolClassIdAndDeletedFalseOrderBySubjectNameAsc(Long classId);

    boolean existsBySubjectCodeIgnoreCase(String subjectCode);
}
