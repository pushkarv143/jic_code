package com.school.sms.repository;

import com.school.sms.entity.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long>, JpaSpecificationExecutor<FeePayment> {

    boolean existsByReceiptNumber(String receiptNumber);

    // Reports: /api/v1/reports/fee-collection — collected amount grouped by fee category.
    @Query("SELECT fp.studentFee.feeStructure.feeCategory.name, COALESCE(SUM(fp.amount), 0) FROM FeePayment fp " +
            "WHERE (:academicYearId IS NULL OR fp.studentFee.academicYear.id = :academicYearId) " +
            "GROUP BY fp.studentFee.feeStructure.feeCategory.name ORDER BY fp.studentFee.feeStructure.feeCategory.name")
    List<Object[]> aggregateCollectedByCategory(@Param("academicYearId") Long academicYearId);

    // Reports: /api/v1/reports/fee-collection — collected amount grouped by payment month (1-12).
    @Query("SELECT MONTH(fp.paymentDate), COALESCE(SUM(fp.amount), 0) FROM FeePayment fp " +
            "WHERE (:academicYearId IS NULL OR fp.studentFee.academicYear.id = :academicYearId) " +
            "GROUP BY MONTH(fp.paymentDate) ORDER BY MONTH(fp.paymentDate)")
    List<Object[]> aggregateCollectedByMonth(@Param("academicYearId") Long academicYearId);
}
