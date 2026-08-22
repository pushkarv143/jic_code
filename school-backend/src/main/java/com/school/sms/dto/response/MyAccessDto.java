package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

/**
 * Everything the UI needs to decide what to offer the signed-in user, in one call.
 *
 * <p>The login response already carries the raw role and permission names, but it
 * is written to {@code localStorage} and therefore goes stale: a permission an
 * administrator grants or revokes, or a module they switch off, would not reach an
 * open session until the next login. This endpoint is fetched on app start and
 * after any change to roles or modules, and is the authoritative answer.
 *
 * <p>Three independent things narrow what a user sees, and all three are here
 * because the UI needs to distinguish them:
 *
 * <ul>
 *   <li>{@code permissions} — the caller's {@code role_permissions} grants, already
 *       filtered to remove anything belonging to a disabled module. A permission
 *       missing here means "do not offer this", whatever the reason.</li>
 *   <li>{@code enabledModules} — which modules are switched on for the
 *       organisation, so a menu section can be dropped wholesale rather than
 *       inferred from the absence of its permissions.</li>
 *   <li>{@code homeroom} — the section this caller is class teacher of, or null.
 *       Not derivable from a permission: it is a row in {@code sections}.</li>
 *   <li>{@code menus} — the navigation tree this caller should be offered, read
 *       from {@code menus} / {@code role_menus} and already filtered by the three
 *       gates above. Included here rather than left to a second call because the
 *       clients need it at exactly the same moment, and because a menu assembled
 *       from a different snapshot than the grants can disagree with them.</li>
 * </ul>
 *
 * <p>Note that permissions are filtered by module but the role is not: a class
 * teacher whose MY_CLASS module is disabled keeps the flag and loses the
 * capability, which is the intended shape.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyAccessDto {

    private Long userId;
    private String username;
    private String role;

    /**
     * Effective permission names, e.g. {@code STUDENT_VIEW} — module-filtered.
     *
     * <p>Never null and never empty for a real login: an empty set is the signal
     * the frontend reads as "grants unknown, fall back to role checks", and
     * returning one here for a user who genuinely holds nothing would quietly
     * re-open every action. Roles with no grants are vanishingly rare, but where
     * they exist the set is empty and the frontend's role gating still applies.
     */
    private Set<String> permissions;

    /** Module keys currently switched on, ordered as the registry orders them. */
    private List<String> enabledModules;

    /** The caller's homeroom section, or null when they are class teacher of none. */
    private HomeroomDto homeroom;

    /**
     * The caller's menu, as a tree of section headings and their entries.
     *
     * <p>Already filtered — every entry here is one this user should see, so a
     * client renders the list as given and makes no decisions of its own. Headings
     * with no surviving entry are omitted rather than returned empty.
     *
     * <p>Empty only for a role assigned nothing, which is a real configuration
     * rather than an error. A client showing an empty sidebar for such a user is
     * correct; there is no fallback to a hard-coded menu, because that fallback is
     * exactly what this replaced.
     */
    private List<MenuDto> menus;

    /**
     * True when this caller may act as a class teacher on their own section —
     * i.e. they hold a homeroom assignment <em>and</em> the MY_CLASS module is on.
     *
     * <p>Precomputed rather than left to the client so the rule lives in one place;
     * the client would otherwise have to re-derive it from three separate fields
     * and could easily get it wrong in one screen out of ten.
     */
    private boolean classTeacherOfOwnSection;
}
