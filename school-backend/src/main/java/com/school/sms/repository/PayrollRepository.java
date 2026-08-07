package com.school.sms.repository;

import com.school.sms.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PayrollRepository extends JpaRepository<Payroll, Long>, JpaSpecificationExecutor<Payroll> {

    boolean existsByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

    /**
     * Dashboard aggregate for /api/v1/payroll/dashboard: [0] = total paid net
     * salary, [1] = total pending net salary, [2] = number of payroll rows,
     * for the given month/year.
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN p.status = 'PAID' THEN p.netSalary ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN p.status = 'PENDING' THEN p.netSalary ELSE 0 END), 0), " +
            "COUNT(p) FROM Payroll p WHERE p.month = :month AND p.year = :year")
    List<Object[]> aggregateDashboard(@Param("month") Integer month, @Param("year") Integer year);

    /**
     * /api/v1/reports/payroll-summary?year=: [0] = total paid amount for the
     * year, [1] = total pending amount for the year.
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN p.status = 'PAID' THEN p.netSalary ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN p.status = 'PENDING' THEN p.netSalary ELSE 0 END), 0) " +
            "FROM Payroll p WHERE p.year = :year")
    List<Object[]> aggregateYearTotals(@Param("year") Integer year);

    // Paid amount grouped by month for a given year, for the payroll-summary byMonth breakdown.
    @Query("SELECT p.month, COALESCE(SUM(p.netSalary), 0) FROM Payroll p " +
            "WHERE p.year = :year AND p.status = 'PAID' GROUP BY p.month ORDER BY p.month")
    List<Object[]> aggregatePaidByMonth(@Param("year") Integer year);
}
