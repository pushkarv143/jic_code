package com.school.sms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.Objects;

/**
 * One student's tenure in one class post.
 *
 * <p>Rows are never overwritten on a change of holder: the sitting official's
 * {@code toDate} is set and a new row opened, so "who was head girl last year"
 * survives. {@code toDate == null} means the appointment is current, and the
 * table's generated {@code current_flag} column makes at most one such row per
 * (class, role) enforceable in the database — see 12_class_module.sql for why
 * a plain unique key over toDate cannot do it.
 */
@Entity
@Table(name = "class_officials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"schoolClass", "section", "student"})
public class ClassOfficial extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    /** Null for a class-wide post; set only where a post is scoped to one section. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private ClassOfficialRole role;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    /** Null while the appointment is current. */
    @Column(name = "to_date")
    private LocalDate toDate;

    @Column(name = "remarks")
    private String remarks;

    public boolean isCurrent() {
        return toDate == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassOfficial that)) return false;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
