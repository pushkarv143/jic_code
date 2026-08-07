package com.school.sms.service.impl;

import com.school.sms.dto.request.ExamTypeRequest;
import com.school.sms.dto.response.ExamTypeDto;
import com.school.sms.entity.ExamType;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.ExamTypeMapper;
import com.school.sms.repository.ExamTypeRepository;
import com.school.sms.service.ExamTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamTypeServiceImpl implements ExamTypeService {

    private final ExamTypeRepository examTypeRepository;
    private final ExamTypeMapper examTypeMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ExamTypeDto> getAll() {
        return examTypeRepository.findAllByOrderByNameAsc().stream()
                .map(examTypeMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ExamTypeDto create(ExamTypeRequest request) {
        if (examTypeRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("ExamType", "name", request.getName());
        }
        ExamType entity = examTypeMapper.toEntity(request);
        return examTypeMapper.toDto(examTypeRepository.save(entity));
    }

    @Override
    @Transactional
    public ExamTypeDto update(Long id, ExamTypeRequest request) {
        ExamType entity = findEntity(id);
        if (!entity.getName().equalsIgnoreCase(request.getName())
                && examTypeRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("ExamType", "name", request.getName());
        }
        examTypeMapper.updateEntityFromRequest(request, entity);
        return examTypeMapper.toDto(examTypeRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        examTypeRepository.delete(findEntity(id));
    }

    private ExamType findEntity(Long id) {
        return examTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExamType", "id", id));
    }
}
