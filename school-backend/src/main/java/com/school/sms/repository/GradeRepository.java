package com.school.sms.repository;

import com.school.sms.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    /**
     * Resolves the single grade row whose [min_percentage, max_percentage]
     * range contains the given percentage. Reused by MarkServiceImpl (mark
     * entry) and ExamServiceImpl (report card / class results) so grade
     * boundaries stay defined only in the seeded `grades` table, never
     * hardcoded in Java.
     */
    Optional<Grade> findFirstByMinPercentageLessThanEqualAndMaxPercentageGreaterThanEqual(
            BigDecimal percentageLowerBound, BigDecimal percentageUpperBound);
}
