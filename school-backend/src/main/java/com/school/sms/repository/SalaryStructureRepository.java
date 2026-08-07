package com.school.sms.repository;

import com.school.sms.entity.SalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SalaryStructureRepository
        extends JpaRepository<SalaryStructure, Long>, JpaSpecificationExecutor<SalaryStructure> {

    // salary_structures.employee_id is UNIQUE per schema — one structure per employee regardless of type.
    Optional<SalaryStructure> findByEmployeeId(Long employeeId);

    boolean existsByEmployeeId(Long employeeId);
}
