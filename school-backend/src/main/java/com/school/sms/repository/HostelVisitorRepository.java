package com.school.sms.repository;

import com.school.sms.entity.HostelVisitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HostelVisitorRepository extends JpaRepository<HostelVisitor, Long>,
        JpaSpecificationExecutor<HostelVisitor> {
}
