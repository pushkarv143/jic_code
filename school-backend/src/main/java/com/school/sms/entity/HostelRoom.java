package com.school.sms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.util.Objects;

/**
 * occupied_count is maintained by trg_hostel_students_after_insert /
 * trg_hostel_students_after_update (see database/04_triggers.sql) whenever a
 * hostel_students row is inserted/updated — Java must never increment/
 * decrement this column itself, only insert/update hostel_students rows and
 * re-read (entityManager.refresh) this entity afterwards.
 */
@Entity
@Table(name = "hostel_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "hostel")
public class HostelRoom extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hostel_id", nullable = false)
    private Hostel hostel;

    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "occupied_count", nullable = false)
    private Integer occupiedCount;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HostelRoom that)) return false;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
