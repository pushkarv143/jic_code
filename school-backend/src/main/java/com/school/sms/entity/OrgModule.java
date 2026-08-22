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
 * One functional module this deployment can run, keyed by the same value the
 * permission catalogue groups on ({@code permissions.module}).
 *
 * <p>Disabling a module removes every permission in it from the effective grant
 * set — see {@code AccessServiceImpl} — which is what makes the menu entries and
 * in-page actions depending on those permissions disappear. The module row is
 * therefore the single switch for a whole area of the product, and the reason
 * authorization is configurable per organisation rather than fixed in seed SQL.
 *
 * <p>{@code core} modules cannot be disabled. Without that guard an
 * administrator could switch off ROLE or SETTINGS and permanently lose the
 * ability to switch anything back on. The flag lives in the database rather than
 * a constant so the protected set is data an organisation can be migrated into,
 * not a code change.
 *
 * @see com.school.sms.entity.Permission#getModule()
 */
@Entity
@Table(name = "org_modules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OrgModule extends AuditableEntity {

    @Column(name = "module_key", nullable = false, unique = true, length = 50)
    private String moduleKey;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "description")
    private String description;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /** True for modules the API refuses to disable; see the class comment. */
    @Column(name = "is_core", nullable = false)
    @Builder.Default
    private boolean core = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 100;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OrgModule module)) {
            return false;
        }
        return getId() != null && Objects.equals(getId(), module.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
