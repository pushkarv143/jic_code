package com.school.sms.repository;

import com.school.sms.entity.StudentMedicalDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentMedicalDetailsRepository extends JpaRepository<StudentMedicalDetails, Long> {

    Optional<StudentMedicalDetails> findByStudentId(Long studentId);
}
