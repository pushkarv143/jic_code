package com.school.sms.service;

import com.school.sms.dto.response.MenuDto;

import java.util.List;
import java.util.Set;

/**
 * The navigation menu, as data.
 *
 * @see com.school.sms.entity.Menu for why a menu assignment is not authority
 */
public interface MenuService {

    /**
     * The signed-in user's menu, as a tree of headings and their children.
     *
     * <p>Already filtered: every entry returned is one this user should see, so a
     * client renders what it is given and decides nothing. Headings with no
     * surviving child are dropped.
     */
    List<MenuDto> getMyMenus();

    /**
     * The same, for a caller who has already resolved the user's context.
     *
     * <p>Exists so {@code /me/access} can include the menu without re-reading the
     * role, the grants and the homeroom it has just looked up. Passing them in
     * also keeps the filter honest — one implementation, one set of rules, whether
     * the caller is the access endpoint or the menu endpoint.
     *
     * @param roleId                 the caller's role
     * @param roleName               used only for the SUPER_ADMIN permission bypass
     * @param effectivePermissions   grants already filtered for disabled modules
     * @param enabledModules         module keys currently switched on
     * @param holdsHomeroomSection   whether the caller is class teacher of a section
     */
    List<MenuDto> getMenusFor(Long roleId,
                              String roleName,
                              Set<String> effectivePermissions,
                              Set<String> enabledModules,
                              boolean holdsHomeroomSection);

    /** Every menu, unfiltered, for the administration screen. */
    List<MenuDto> getCatalogue();

    /** Menu ids currently assigned to one role, disabled menus included. */
    Set<Long> getRoleMenuIds(Long roleId);

    /** Replaces one role's menu assignment wholesale. Returns the new set. */
    Set<Long> replaceRoleMenus(Long roleId, Set<Long> menuIds);
}
