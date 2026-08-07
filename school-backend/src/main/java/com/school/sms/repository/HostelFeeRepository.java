package com.school.sms.repository;

import com.school.sms.entity.HostelFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HostelFeeRepository extends JpaRepository<HostelFee, Long>, JpaSpecificationExecutor<HostelFee> {

    boolean existsByStudentIdAndMonthAndYear(Long studentId, Integer month, Integer year);
}
