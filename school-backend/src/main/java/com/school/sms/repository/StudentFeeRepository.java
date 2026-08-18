package com.school.sms.repository;

import com.school.sms.entity.StudentFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentFeeRepository extends JpaRepository<StudentFee, Long>, JpaSpecificationExecutor<StudentFee> {

    boolean existsByStudentIdAndFeeStructureId(Long studentId, Long feeStructureId);

    Optional<StudentFee> findByStudentIdAndFeeStructureId(Long studentId, Long feeStructureId);

    /**
     * Dashboard aggregate for /api/v1/fees/dues-summary. Returns a single row:
     * [0] = total amount due, [1] = total amount collected, [2] = distinct
     * student count — either filter may be null to mean "no filter".
     */
    @Query("SELECT COALESCE(SUM(sf.amountDue), 0), COALESCE(SUM(sf.amountPaid), 0), COUNT(DISTINCT sf.student.id) " +
            "FROM StudentFee sf " +
            "WHERE (:classId IS NULL OR sf.student.schoolClass.id = :classId) " +
            "AND (:academicYearId IS NULL OR sf.academicYear.id = :academicYearId)")
    List<Object[]> aggregateDuesSummary(@Param("classId") Long classId, @Param("academicYearId") Long academicYearId);

    /**
     * Class-overview fee panel: a single row of [distinct defaulting students,
     * total outstanding] for one class. A defaulter is a student carrying at
     * least one row that is not yet PAID; the outstanding figure sums
     * amountDue - amountPaid over those same rows, so a partly-paid fee
     * contributes only its remainder rather than its face value.
     */
    @Query("SELECT COUNT(DISTINCT sf.student.id), COALESCE(SUM(sf.amountDue - sf.amountPaid), 0) " +
            "FROM StudentFee sf WHERE sf.student.schoolClass.id = :classId " +
            "AND sf.student.deleted = false AND sf.status <> com.school.sms.entity.FeeStatus.PAID")
    List<Object[]> aggregateDefaultersForClass(@Param("classId") Long classId);
}
