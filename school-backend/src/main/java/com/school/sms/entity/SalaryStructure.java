package com.school.sms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * employee_id is a raw FK to users.id (no JPA relationship) — same rationale
 * as LeaveApplication.applicantId: the referenced row may live in either the
 * teachers or staff table depending on employee_type, so there's no single
 * JPA association target. Callers resolve the display name/department via
 * TeacherRepository/StaffRepository lookups keyed by user id.
 */
@Entity
@Table(name = "salary_structures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SalaryStructure extends AuditableEntity {

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "employee_type", nullable = false, length = 20)
    private PayrollEmployeeType employeeType;

    @Column(name = "basic_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "hra", nullable = false, precision = 12, scale = 2)
    private BigDecimal hra;

    @Column(name = "da", nullable = false, precision = 12, scale = 2)
    private BigDecimal da;

    @Column(name = "other_allowances", nullable = false, precision = 12, scale = 2)
    private BigDecimal otherAllowances;

    @Column(name = "pf_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal pfPercentage;

    @Column(name = "esi_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal esiPercentage;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SalaryStructure that)) return false;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
