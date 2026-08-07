package com.school.sms.repository;

import com.school.sms.entity.LeaveApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LeaveApplicationRepository
        extends JpaRepository<LeaveApplication, Long>, JpaSpecificationExecutor<LeaveApplication> {

    Page<LeaveApplication> findAllByApplicantId(Long applicantId, Pageable pageable);
}
