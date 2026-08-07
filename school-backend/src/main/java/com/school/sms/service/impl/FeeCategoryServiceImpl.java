package com.school.sms.service.impl;

import com.school.sms.dto.request.FeeCategoryRequest;
import com.school.sms.dto.response.FeeCategoryDto;
import com.school.sms.entity.FeeCategory;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.FeeCategoryMapper;
import com.school.sms.repository.FeeCategoryRepository;
import com.school.sms.service.FeeCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeCategoryServiceImpl implements FeeCategoryService {

    private final FeeCategoryRepository feeCategoryRepository;
    private final FeeCategoryMapper feeCategoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FeeCategoryDto> getAll() {
        return feeCategoryRepository.findAllByOrderByNameAsc().stream()
                .map(feeCategoryMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public FeeCategoryDto create(FeeCategoryRequest request) {
        if (feeCategoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("FeeCategory", "name", request.getName());
        }
        FeeCategory entity = feeCategoryMapper.toEntity(request);
        return feeCategoryMapper.toDto(feeCategoryRepository.save(entity));
    }

    @Override
    @Transactional
    public FeeCategoryDto update(Long id, FeeCategoryRequest request) {
        FeeCategory entity = findEntity(id);
        if (!entity.getName().equalsIgnoreCase(request.getName())
                && feeCategoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("FeeCategory", "name", request.getName());
        }
        feeCategoryMapper.updateEntityFromRequest(request, entity);
        return feeCategoryMapper.toDto(feeCategoryRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        feeCategoryRepository.delete(findEntity(id));
    }

    private FeeCategory findEntity(Long id) {
        return feeCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FeeCategory", "id", id));
    }
}
