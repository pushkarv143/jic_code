package com.school.sms.service.impl;

import com.school.sms.dto.request.DesignationRequest;
import com.school.sms.dto.response.DesignationDto;
import com.school.sms.entity.Designation;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.DesignationMapper;
import com.school.sms.repository.DesignationRepository;
import com.school.sms.service.DesignationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DesignationServiceImpl implements DesignationService {

    private final DesignationRepository designationRepository;
    private final DesignationMapper designationMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DesignationDto> getAll() {
        return designationRepository.findAllByOrderByNameAsc().stream()
                .map(designationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DesignationDto getById(Long id) {
        return designationMapper.toDto(findEntity(id));
    }

    @Override
    @Transactional
    public DesignationDto create(DesignationRequest request) {
        if (designationRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Designation", "name", request.getName());
        }
        Designation entity = designationMapper.toEntity(request);
        return designationMapper.toDto(designationRepository.save(entity));
    }

    @Override
    @Transactional
    public DesignationDto update(Long id, DesignationRequest request) {
        Designation entity = findEntity(id);
        if (!entity.getName().equalsIgnoreCase(request.getName())
                && designationRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Designation", "name", request.getName());
        }
        designationMapper.updateEntityFromRequest(request, entity);
        return designationMapper.toDto(designationRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        designationRepository.delete(findEntity(id));
    }

    private Designation findEntity(Long id) {
        return designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", id));
    }
}
