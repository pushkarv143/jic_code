package com.school.sms.repository;

import com.school.sms.entity.FeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface FeeStructureRepository
        extends JpaRepository<FeeStructure, Long>, JpaSpecificationExecutor<FeeStructure> {

    boolean existsBySchoolClassIdAndAcademicYearIdAndFeeCategoryId(Long classId, Long academicYearId, Long feeCategoryId);

    List<FeeStructure> findAllByIdInAndSchoolClassIdAndAcademicYearId(List<Long> ids, Long classId, Long academicYearId);
}
