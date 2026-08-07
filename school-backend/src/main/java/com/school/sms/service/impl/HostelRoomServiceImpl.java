package com.school.sms.service.impl;

import com.school.sms.dto.request.HostelRoomRequest;
import com.school.sms.dto.response.HostelRoomDto;
import com.school.sms.entity.Hostel;
import com.school.sms.entity.HostelRoom;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.HostelRoomMapper;
import com.school.sms.repository.HostelRepository;
import com.school.sms.repository.HostelRoomRepository;
import com.school.sms.service.HostelRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HostelRoomServiceImpl implements HostelRoomService {

    private final HostelRoomRepository hostelRoomRepository;
    private final HostelRepository hostelRepository;
    private final HostelRoomMapper hostelRoomMapper;

    @Override
    @Transactional(readOnly = true)
    public List<HostelRoomDto> getByHostel(Long hostelId) {
        findHostel(hostelId);
        return hostelRoomRepository.findAllByHostelIdOrderByRoomNumberAsc(hostelId).stream()
                .map(hostelRoomMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public HostelRoomDto create(Long hostelId, HostelRoomRequest request) {
        Hostel hostel = findHostel(hostelId);
        if (hostelRoomRepository.existsByHostelIdAndRoomNumberIgnoreCase(hostelId, request.getRoomNumber())) {
            throw new DuplicateResourceException("HostelRoom", "roomNumber", request.getRoomNumber());
        }

        HostelRoom room = HostelRoom.builder()
                .hostel(hostel)
                .roomNumber(request.getRoomNumber())
                .capacity(request.getCapacity())
                .occupiedCount(0)
                .build();

        return hostelRoomMapper.toDto(hostelRoomRepository.save(room));
    }

    @Override
    @Transactional
    public HostelRoomDto update(Long id, HostelRoomRequest request) {
        HostelRoom room = findEntity(id);
        if (!room.getRoomNumber().equalsIgnoreCase(request.getRoomNumber())
                && hostelRoomRepository.existsByHostelIdAndRoomNumberIgnoreCase(room.getHostel().getId(), request.getRoomNumber())) {
            throw new DuplicateResourceException("HostelRoom", "roomNumber", request.getRoomNumber());
        }

        room.setRoomNumber(request.getRoomNumber());
        room.setCapacity(request.getCapacity());

        return hostelRoomMapper.toDto(hostelRoomRepository.save(room));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        hostelRoomRepository.delete(findEntity(id));
    }

    private HostelRoom findEntity(Long id) {
        return hostelRoomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HostelRoom", "id", id));
    }

    private Hostel findHostel(Long id) {
        return hostelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hostel", "id", id));
    }
}
