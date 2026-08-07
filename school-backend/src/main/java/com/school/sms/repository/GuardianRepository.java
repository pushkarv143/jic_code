package com.school.sms.repository;

import com.school.sms.entity.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    List<Guardian> findAllByStudentIdOrderByIdAsc(Long studentId);
}
