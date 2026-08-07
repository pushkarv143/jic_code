package com.school.sms.service;

import com.school.sms.dto.request.HostelRoomRequest;
import com.school.sms.dto.response.HostelRoomDto;

import java.util.List;

public interface HostelRoomService {

    List<HostelRoomDto> getByHostel(Long hostelId);

    HostelRoomDto create(Long hostelId, HostelRoomRequest request);

    HostelRoomDto update(Long id, HostelRoomRequest request);

    void delete(Long id);
}
