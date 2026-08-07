package com.school.sms.repository;

import com.school.sms.entity.Scholarship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ScholarshipRepository extends JpaRepository<Scholarship, Long>, JpaSpecificationExecutor<Scholarship> {
}
