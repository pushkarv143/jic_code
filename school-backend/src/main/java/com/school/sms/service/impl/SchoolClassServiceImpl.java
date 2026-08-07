package com.school.sms.service.impl;

import com.school.sms.dto.request.SchoolClassRequest;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.SchoolClassDto;
import com.school.sms.entity.AcademicYear;
import com.school.sms.entity.SchoolClass;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.SchoolClassMapper;
import com.school.sms.repository.AcademicYearRepository;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.SubjectRepository;
import com.school.sms.service.SchoolClassService;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SchoolClassServiceImpl implements SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final SchoolClassMapper schoolClassMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SchoolClassDto> getAll(Long academicYearId, int page, int size,
                                                String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProperty = StringUtils.hasText(sortBy) ? sortBy : "id";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        Specification<SchoolClass> spec = new SpecificationBuilder<SchoolClass>()
                .with("deleted", SearchOperation.EQUALS, false)
                .with(academicYearId != null, "academicYear.id", SearchOperation.EQUALS, academicYearId)
                .build();

        Page<SchoolClass> pageResult = schoolClassRepository.findAll(spec, pageable);
        Page<SchoolClassDto> dtoPage = pageResult.map(schoolClassMapper::toDto);
        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolClassDto getById(Long id) {
        SchoolClass entity = findEntity(id);
        SchoolClassDto dto = schoolClassMapper.toDto(entity);
        dto.setSectionCount(sectionRepository.findAllBySchoolClassIdOrderBySectionNameAsc(id).size());
        dto.setSubjectCount(subjectRepository.findAllBySchoolClassIdAndDeletedFalseOrderBySubjectNameAsc(id).size());
        return dto;
    }

    @Override
    @Transactional
    public SchoolClassDto create(SchoolClassRequest request) {
        if (schoolClassRepository.existsByClassNameIgnoreCaseAndAcademicYearIdAndDeletedFalse(
                request.getClassName(), request.getAcademicYearId())) {
            throw new DuplicateResourceException("Class", "className", request.getClassName());
        }
        AcademicYear academicYear = findAcademicYear(request.getAcademicYearId());

        SchoolClass entity = schoolClassMapper.toEntity(request);
        entity.setAcademicYear(academicYear);
        entity.setDeleted(false);

        return schoolClassMapper.toDto(schoolClassRepository.save(entity));
    }

    @Override
    @Transactional
    public SchoolClassDto update(Long id, SchoolClassRequest request) {
        SchoolClass entity = findEntity(id);

        boolean nameOrYearChanged = !entity.getClassName().equalsIgnoreCase(request.getClassName())
                || !entity.getAcademicYear().getId().equals(request.getAcademicYearId());
        if (nameOrYearChanged && schoolClassRepository.existsByClassNameIgnoreCaseAndAcademicYearIdAndDeletedFalse(
                request.getClassName(), request.getAcademicYearId())) {
            throw new DuplicateResourceException("Class", "className", request.getClassName());
        }

        AcademicYear academicYear = findAcademicYear(request.getAcademicYearId());
        schoolClassMapper.updateEntityFromRequest(request, entity);
        entity.setAcademicYear(academicYear);

        return schoolClassMapper.toDto(schoolClassRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SchoolClass entity = findEntity(id);
        entity.setDeleted(true);
        schoolClassRepository.save(entity);
    }

    private SchoolClass findEntity(Long id) {
        return schoolClassRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", id));
    }

    private AcademicYear findAcademicYear(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id));
    }
}
