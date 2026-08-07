package com.school.sms.repository;

import com.school.sms.entity.ExamType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamTypeRepository extends JpaRepository<ExamType, Long> {

    List<ExamType> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
