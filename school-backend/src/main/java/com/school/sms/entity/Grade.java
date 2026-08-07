package com.school.sms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Reference data, seeded via database/06_seed_reference_data.sql (A1 91-100 ...
 * E 0-32.99). Not exposed via its own CRUD endpoints in this round — only
 * used internally by MarkServiceImpl/ExamServiceImpl to auto-resolve a grade
 * from a computed percentage.
 */
@Entity
@Table(name = "grades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Grade extends AuditableEntity {

    @Column(name = "grade_name", nullable = false, unique = true, length = 10)
    private String gradeName;

    @Column(name = "min_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal minPercentage;

    @Column(name = "max_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxPercentage;

    @Column(name = "grade_point", nullable = false, precision = 3, scale = 1)
    private BigDecimal gradePoint;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Grade that)) return false;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
