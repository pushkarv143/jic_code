package com.school.sms.repository;

import com.school.sms.entity.StudentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentDocumentRepository extends JpaRepository<StudentDocument, Long> {

    List<StudentDocument> findAllByStudentIdOrderByIdDesc(Long studentId);
}
