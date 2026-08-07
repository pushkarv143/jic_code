package com.school.sms.repository;

import com.school.sms.entity.Staff;
import com.school.sms.entity.TeacherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long>, JpaSpecificationExecutor<Staff> {

    Optional<Staff> findByUserId(Long userId);

    // Payroll generation: roster of active staff of a given status (reuses TeacherStatus, see Staff entity javadoc).
    List<Staff> findAllByDeletedFalseAndStatus(TeacherStatus status);
}
