package com.school.sms.repository;

import com.school.sms.entity.HostelRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HostelRoomRepository extends JpaRepository<HostelRoom, Long> {

    List<HostelRoom> findAllByHostelIdOrderByRoomNumberAsc(Long hostelId);

    boolean existsByHostelIdAndRoomNumberIgnoreCase(Long hostelId, String roomNumber);
}
