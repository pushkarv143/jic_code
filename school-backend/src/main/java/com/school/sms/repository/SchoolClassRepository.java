package com.school.sms.repository;

import com.school.sms.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long>, JpaSpecificationExecutor<SchoolClass> {

    boolean existsByClassNameIgnoreCaseAndAcademicYearIdAndDeletedFalse(String className, Long academicYearId);

    // Excel student import: resolves a row's plain-text class name to an id, scoped to
    // the current academic year (import doesn't accept an academicYearId column).
    Optional<SchoolClass> findFirstByClassNameIgnoreCaseAndAcademicYearIdAndDeletedFalse(String className, Long academicYearId);
}
