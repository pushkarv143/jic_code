package com.school.sms.repository;

import com.school.sms.entity.OnlineClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OnlineClassRepository extends JpaRepository<OnlineClass, Long>, JpaSpecificationExecutor<OnlineClass> {
}
