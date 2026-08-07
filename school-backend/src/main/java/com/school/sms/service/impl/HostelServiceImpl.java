package com.school.sms.service.impl;

import com.school.sms.dto.request.HostelRequest;
import com.school.sms.dto.response.HostelDto;
import com.school.sms.entity.Hostel;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.HostelMapper;
import com.school.sms.repository.HostelRepository;
import com.school.sms.service.HostelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HostelServiceImpl implements HostelService {

    private final HostelRepository hostelRepository;
    private final HostelMapper hostelMapper;

    @Override
    @Transactional(readOnly = true)
    public List<HostelDto> getAll() {
        return hostelRepository.findAllByOrderByNameAsc().stream()
                .map(hostelMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public HostelDto create(HostelRequest request) {
        Hostel entity = hostelMapper.toEntity(request);
        return hostelMapper.toDto(hostelRepository.save(entity));
    }

    @Override
    @Transactional
    public HostelDto update(Long id, HostelRequest request) {
        Hostel entity = findEntity(id);
        hostelMapper.updateEntityFromRequest(request, entity);
        return hostelMapper.toDto(hostelRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        hostelRepository.delete(findEntity(id));
    }

    private Hostel findEntity(Long id) {
        return hostelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hostel", "id", id));
    }
}
