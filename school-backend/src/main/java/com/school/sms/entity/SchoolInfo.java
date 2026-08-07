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

import java.util.Objects;

/**
 * Singleton settings row: the table has no natural unique business key, so by
 * convention the row with id=1 (the first and only one ever inserted) is
 * treated as THE school info record. See SchoolInfoServiceImpl for the
 * "create a default row if missing" get-or-create logic.
 */
@Entity
@Table(name = "school_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SchoolInfo extends AuditableEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "established_year")
    private Integer establishedYear;

    @Column(name = "affiliation_number", length = 100)
    private String affiliationNumber;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SchoolInfo that)) return false;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
