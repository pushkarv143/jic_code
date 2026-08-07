package com.school.sms.repository;

import com.school.sms.entity.SchoolInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolInfoRepository extends JpaRepository<SchoolInfo, Long> {
}
