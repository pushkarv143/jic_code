package com.school.sms.repository;

import com.school.sms.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findAllByOrderByNameAsc();

    boolean existsByLicenseNumber(String licenseNumber);
}
