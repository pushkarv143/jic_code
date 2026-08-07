package com.school.sms.repository;

import com.school.sms.entity.PickupPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PickupPointRepository extends JpaRepository<PickupPoint, Long> {

    List<PickupPoint> findAllByRouteIdOrderByPickupTimeAsc(Long routeId);
}
