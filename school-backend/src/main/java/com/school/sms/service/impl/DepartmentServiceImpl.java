package com.school.sms.service.impl;

import com.school.sms.dto.request.DepartmentRequest;
import com.school.sms.dto.response.DepartmentDto;
import com.school.sms.entity.Department;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.DepartmentMapper;
import com.school.sms.repository.DepartmentRepository;
import com.school.sms.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDto> getAll() {
        return departmentRepository.findAllByOrderByNameAsc().stream()
                .map(departmentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDto getById(Long id) {
        return departmentMapper.toDto(findEntity(id));
    }

    @Override
    @Transactional
    public DepartmentDto create(DepartmentRequest request) {
        if (departmentRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Department", "name", request.getName());
        }
        Department entity = departmentMapper.toEntity(request);
        return departmentMapper.toDto(departmentRepository.save(entity));
    }

    @Override
    @Transactional
    public DepartmentDto update(Long id, DepartmentRequest request) {
        Department entity = findEntity(id);
        if (!entity.getName().equalsIgnoreCase(request.getName())
                && departmentRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Department", "name", request.getName());
        }
        departmentMapper.updateEntityFromRequest(request, entity);
        return departmentMapper.toDto(departmentRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        departmentRepository.delete(findEntity(id));
    }

    private Department findEntity(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
    }
}
