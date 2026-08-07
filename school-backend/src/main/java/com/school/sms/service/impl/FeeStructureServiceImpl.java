package com.school.sms.service.impl;

import com.school.sms.dto.request.FeeStructureRequest;
import com.school.sms.dto.response.FeeStructureDto;
import com.school.sms.entity.AcademicYear;
import com.school.sms.entity.FeeCategory;
import com.school.sms.entity.FeeStructure;
import com.school.sms.entity.SchoolClass;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.FeeStructureMapper;
import com.school.sms.repository.AcademicYearRepository;
import com.school.sms.repository.FeeCategoryRepository;
import com.school.sms.repository.FeeStructureRepository;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.service.FeeStructureService;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeStructureServiceImpl implements FeeStructureService {

    private final FeeStructureRepository feeStructureRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AcademicYearRepository academicYearRepository;
    private final FeeCategoryRepository feeCategoryRepository;
    private final FeeStructureMapper feeStructureMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FeeStructureDto> getAll(Long classId, Long academicYearId, Long feeCategoryId) {
        Specification<FeeStructure> spec = new SpecificationBuilder<FeeStructure>()
                .with(classId != null, "schoolClass.id", SearchOperation.EQUALS, classId)
                .with(academicYearId != null, "academicYear.id", SearchOperation.EQUALS, academicYearId)
                .with(feeCategoryId != null, "feeCategory.id", SearchOperation.EQUALS, feeCategoryId)
                .build();

        return feeStructureRepository.findAll(spec).stream()
                .map(feeStructureMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public FeeStructureDto create(FeeStructureRequest request) {
        SchoolClass schoolClass = findClass(request.getClassId());
        AcademicYear academicYear = findAcademicYear(request.getAcademicYearId());
        FeeCategory feeCategory = findFeeCategory(request.getFeeCategoryId());

        if (feeStructureRepository.existsBySchoolClassIdAndAcademicYearIdAndFeeCategoryId(
                request.getClassId(), request.getAcademicYearId(), request.getFeeCategoryId())) {
            throw new DuplicateResourceException(
                    "A fee structure for this class, academic year and fee category already exists");
        }

        FeeStructure entity = FeeStructure.builder()
                .schoolClass(schoolClass)
                .academicYear(academicYear)
                .feeCategory(feeCategory)
                .amount(request.getAmount())
                .dueDate(request.getDueDate())
                .build();

        return feeStructureMapper.toDto(feeStructureRepository.save(entity));
    }

    @Override
    @Transactional
    public FeeStructureDto update(Long id, FeeStructureRequest request) {
        FeeStructure entity = findEntity(id);
        SchoolClass schoolClass = findClass(request.getClassId());
        AcademicYear academicYear = findAcademicYear(request.getAcademicYearId());
        FeeCategory feeCategory = findFeeCategory(request.getFeeCategoryId());

        boolean combinationChanged = !entity.getSchoolClass().getId().equals(request.getClassId())
                || !entity.getAcademicYear().getId().equals(request.getAcademicYearId())
                || !entity.getFeeCategory().getId().equals(request.getFeeCategoryId());
        if (combinationChanged && feeStructureRepository.existsBySchoolClassIdAndAcademicYearIdAndFeeCategoryId(
                request.getClassId(), request.getAcademicYearId(), request.getFeeCategoryId())) {
            throw new DuplicateResourceException(
                    "A fee structure for this class, academic year and fee category already exists");
        }

        entity.setSchoolClass(schoolClass);
        entity.setAcademicYear(academicYear);
        entity.setFeeCategory(feeCategory);
        entity.setAmount(request.getAmount());
        entity.setDueDate(request.getDueDate());

        return feeStructureMapper.toDto(feeStructureRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        feeStructureRepository.delete(findEntity(id));
    }

    private FeeStructure findEntity(Long id) {
        return feeStructureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FeeStructure", "id", id));
    }

    private SchoolClass findClass(Long id) {
        return schoolClassRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", id));
    }

    private AcademicYear findAcademicYear(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id));
    }

    private FeeCategory findFeeCategory(Long id) {
        return feeCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FeeCategory", "id", id));
    }
}
