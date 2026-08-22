package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * One menu entry as the clients render it.
 *
 * <p>Serves two endpoints with the same shape on purpose. {@code GET /me/menus}
 * returns the signed-in user's menu, already filtered — every entry in it is one
 * they should see, so a client renders the list as given and makes no decisions.
 * {@code GET /menus} returns the whole catalogue for the administration screen,
 * unfiltered, where the gate fields below are what the screen is editing.
 *
 * <p>The gate fields ({@code moduleKey}, {@code requiredPermission},
 * {@code requiresHomeroom}) are included in both. On the filtered response they
 * are redundant by construction — but they let a client explain why an entry is
 * absent, and returning a different shape per endpoint would cost more than the
 * three fields save.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuDto {

    private Long id;

    /** Stable identifier, e.g. {@code STUDENTS}. Match on this, not the label. */
    private String menuKey;

    private String label;

    /** Null for a section heading. */
    private String path;

    private String icon;

    private String i18nKey;

    private int sortOrder;

    private boolean enabled;

    /** Children of a heading. Empty for a destination. */
    private List<MenuDto> children;

    /* ---- the gates, for the administration screen ---- */

    /** {@code org_modules.module_key}, or null to belong to no module. */
    private String moduleKey;

    /** {@code permissions.name}, or null to need no grant. */
    private String requiredPermission;

    private boolean requiresHomeroom;

    /**
     * How many roles this menu is assigned to. Populated on the catalogue
     * response only — on a user's own menu it would answer a question nobody
     * asked, and cost a query per request to do it.
     */
    private Long assignedRoleCount;
}
