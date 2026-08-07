package com.school.sms.repository;

import com.school.sms.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusRepository extends JpaRepository<Bus, Long> {

    List<Bus> findAllByDeletedFalseOrderByBusNumberAsc();

    boolean existsByBusNumber(String busNumber);

    boolean existsByRegistrationNumber(String registrationNumber);

    // Reports: /api/v1/reports/transport-summary.
    long countByDeletedFalse();
}
