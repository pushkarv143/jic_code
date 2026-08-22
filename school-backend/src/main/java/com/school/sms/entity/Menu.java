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

/**
 * One navigation entry — a destination, or a section heading grouping others.
 *
 * <p>The menu used to live twice over: {@code NAV_GROUPS} in the web app's
 * navConfig.tsx and {@code MENU_ENTRIES} in the Android app's Destinations.kt,
 * each carrying its own hard-coded list of roles. Two files, two languages, and
 * two releases to change who sees what — so in practice nobody did, and every
 * role was offered roughly the same menu whether it meant anything to them or
 * not. These rows are that list, and {@code role_menus} is the assignment.
 *
 * <h2>A menu row is not authority</h2>
 *
 * <p>Being assigned a menu means "this entry may appear", never "this user may
 * act". Three gates still apply on top of the assignment, and every one of them
 * can hide an assigned entry:
 *
 * <ul>
 *   <li>{@link #moduleKey} — is this area switched on for the organisation?
 *       Checked first and overrides everyone, SUPER_ADMIN included, because it is
 *       a statement about the school rather than about the user.</li>
 *   <li>{@link #requiresHomeroom} — My Class, for a teacher who actually holds a
 *       section in {@code sections.class_teacher_id}. Not derivable from a role:
 *       far more users carry the CLASS_TEACHER label than hold a section.</li>
 *   <li>{@link #requiredPermission} — the {@code role_permissions} grant behind
 *       the screen. SUPER_ADMIN bypasses this one, matching ADMIN_OVERRIDE.</li>
 * </ul>
 *
 * <p>So an assignment can only ever narrow the menu, never widen access. Granting
 * a menu to a role that lacks the permission behind it shows nothing at all —
 * which is the intended failure mode, since the API would refuse the call anyway.
 *
 * @see com.school.sms.service.impl.MenuServiceImpl for where the gates are applied
 */
@Entity
@Table(name = "menus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Menu extends AuditableEntity {

    /**
     * Stable identifier the clients match on, e.g. {@code STUDENTS}.
     *
     * <p>Deliberately not the label or the path: both can change — a screen gets
     * renamed, a route moves — without either app needing to know. This cannot,
     * which is why code refers to it and never to the other two.
     */
    @Column(name = "menu_key", nullable = false, unique = true, length = 64)
    private String menuKey;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    /** Null for a section heading, which groups rather than navigates. */
    @Column(name = "path", length = 150)
    private String path;

    /** Icon name resolved per client — one string serves web and Android. */
    @Column(name = "icon", length = 64)
    private String icon;

    /**
     * Parent heading, or null for a top-level heading.
     *
     * <p>Held as a plain id rather than a {@code @ManyToOne}: the tree is read
     * whole, once per request, and assembled in memory. A managed association
     * would add a lazy proxy and an N+1 risk for a relationship that is only ever
     * used to group 28 rows.
     */
    @Column(name = "parent_id")
    private Long parentId;

    /** {@code org_modules.module_key}, or null to belong to no module. */
    @Column(name = "module_key", length = 50)
    private String moduleKey;

    /** {@code permissions.name}, or null to need no grant beyond signing in. */
    @Column(name = "required_permission", length = 100)
    private String requiredPermission;

    @Column(name = "requires_homeroom", nullable = false)
    @Builder.Default
    private boolean requiresHomeroom = false;

    /** Dot-path into the clients' translation tables, e.g. {@code nav.students}. */
    @Column(name = "i18n_key", length = 100)
    private String i18nKey;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    /**
     * Switches an entry off for every role at once, without deleting the row or
     * its assignments. For retiring a screen — Chat is seeded this way, being a
     * placeholder with no controller behind it.
     */
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /** True for a section heading: something that groups rather than navigates. */
    public boolean isHeading() {
        return parentId == null;
    }
}
