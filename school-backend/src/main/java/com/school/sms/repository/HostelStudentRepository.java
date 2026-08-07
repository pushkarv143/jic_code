package com.school.sms.repository;

import com.school.sms.entity.HostelStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HostelStudentRepository extends JpaRepository<HostelStudent, Long>,
        JpaSpecificationExecutor<HostelStudent> {
}
