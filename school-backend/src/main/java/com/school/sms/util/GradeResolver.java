package com.school.sms.util;

import com.school.sms.entity.Grade;
import com.school.sms.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Resolves a {@link Grade} from a computed percentage by looking up the
 * seeded `grades` reference table (min_percentage/max_percentage ranges) —
 * reused by MarkServiceImpl (mark entry) and ExamServiceImpl (report card /
 * class results) so grade boundaries are defined only in the database, never
 * hardcoded in Java.
 */
@Component
@RequiredArgsConstructor
public class GradeResolver {

    private final GradeRepository gradeRepository;

    /** Resolves the grade for marksObtained out of maxMarks (returns null if maxMarks is not positive). */
    public Grade resolve(BigDecimal marksObtained, int maxMarks) {
        if (maxMarks <= 0 || marksObtained == null) {
            return null;
        }
        BigDecimal percentage = marksObtained
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(maxMarks), 2, RoundingMode.HALF_UP);
        return resolveByPercentage(percentage);
    }

    public Grade resolveByPercentage(BigDecimal percentage) {
        if (percentage == null) {
            return null;
        }
        return gradeRepository
                .findFirstByMinPercentageLessThanEqualAndMaxPercentageGreaterThanEqual(percentage, percentage)
                .orElse(null);
    }
}
