package com.school.sms.service.impl;

import com.school.sms.dto.request.AcademicYearRequest;
import com.school.sms.dto.response.AcademicYearDto;
import com.school.sms.entity.AcademicYear;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.AcademicYearMapper;
import com.school.sms.repository.AcademicYearRepository;
import com.school.sms.service.AcademicYearService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicYearServiceImpl implements AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final AcademicYearMapper academicYearMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AcademicYearDto> getAll() {
        return academicYearRepository.findAllByOrderByStartDateDesc().stream()
                .map(academicYearMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicYearDto getById(Long id) {
        return academicYearMapper.toDto(findEntity(id));
    }

    @Override
    @Transactional
    public AcademicYearDto create(AcademicYearRequest request) {
        if (academicYearRepository.existsByYearName(request.getYearName())) {
            throw new DuplicateResourceException("AcademicYear", "yearName", request.getYearName());
        }
        AcademicYear entity = academicYearMapper.toEntity(request);
        entity.setCurrent(false);
        return academicYearMapper.toDto(academicYearRepository.save(entity));
    }

    @Override
    @Transactional
    public AcademicYearDto update(Long id, AcademicYearRequest request) {
        AcademicYear entity = findEntity(id);
        if (!entity.getYearName().equalsIgnoreCase(request.getYearName())
                && academicYearRepository.existsByYearName(request.getYearName())) {
            throw new DuplicateResourceException("AcademicYear", "yearName", request.getYearName());
        }
        academicYearMapper.updateEntityFromRequest(request, entity);
        return academicYearMapper.toDto(academicYearRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AcademicYear entity = findEntity(id);
        academicYearRepository.delete(entity);
    }

    @Override
    @Transactional
    public AcademicYearDto setCurrent(Long id) {
        AcademicYear target = findEntity(id);

        List<AcademicYear> all = academicYearRepository.findAll();
        all.forEach(year -> year.setCurrent(year.getId().equals(id)));
        academicYearRepository.saveAll(all);

        return academicYearMapper.toDto(target);
    }

    private AcademicYear findEntity(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id));
    }
}
