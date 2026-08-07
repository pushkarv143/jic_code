package com.school.sms.repository;

import com.school.sms.entity.AdmissionEnquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdmissionEnquiryRepository extends JpaRepository<AdmissionEnquiry, Long>, JpaSpecificationExecutor<AdmissionEnquiry> {
}
