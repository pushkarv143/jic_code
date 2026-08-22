package com.school.sms.service.impl;

import com.school.sms.dto.response.MenuDto;
import com.school.sms.entity.Menu;
import com.school.sms.entity.OrgModule;
import com.school.sms.entity.Permission;
import com.school.sms.entity.Role;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.MenuRepository;
import com.school.sms.repository.OrgModuleRepository;
import com.school.sms.repository.PermissionRepository;
import com.school.sms.repository.RoleRepository;
import com.school.sms.security.HomeroomGuard;
import com.school.sms.security.SecurityUtils;
import com.school.sms.security.UserPrincipal;
import com.school.sms.service.MenuService;
import com.school.sms.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds a user's menu, and lets an administrator change who gets what.
 *
 * <h2>The filter</h2>
 *
 * <p>Four gates, applied in this order because they get progressively more
 * specific — and because the order is the explanation when someone asks why an
 * entry is missing:
 *
 * <ol>
 *   <li><b>assignment</b> — is this menu in the role's {@code role_menus}? The
 *       coarse shape of the menu, and the only one an administrator edits directly.</li>
 *   <li><b>module</b> — is the area switched on for the organisation? Overrides
 *       everyone including SUPER_ADMIN: a school that runs no hostel wants no
 *       Hostel entry on anybody's menu, the administrator's included.</li>
 *   <li><b>homeroom</b> — for My Class, does this user hold a section?</li>
 *   <li><b>permission</b> — the grant behind the screen. SUPER_ADMIN bypasses
 *       this one, matching {@link AppConstants#ADMIN_OVERRIDE}.</li>
 * </ol>
 *
 * <p>Note what the assignment gate cannot do: it cannot grant access. A role
 * assigned a menu whose permission it lacks sees nothing, and that is deliberate
 * — an entry that leads to a 403 is worse than no entry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final OrgModuleRepository orgModuleRepository;
    private final HomeroomGuard homeroomGuard;

    @Override
    @Transactional(readOnly = true)
    public List<MenuDto> getMyMenus() {
        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new AccessDeniedException("No authenticated user found"));

        Role role = roleRepository.findByName(principal.getRoleName())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", principal.getRoleName()));

        Set<String> disabledModules = Set.copyOf(orgModuleRepository.findDisabledModuleKeys());

        // Read the grants from the database rather than from the principal, for the
        // same reason /me/access does: the principal was built from a JWT that may
        // predate an administrator's change, and this endpoint exists to be current.
        Set<String> effective = permissionRepository.findAllByRoleId(role.getId()).stream()
                .filter(permission -> !disabledModules.contains(permission.getModule()))
                .map(Permission::getName)
                .collect(Collectors.toSet());

        Set<String> enabledModules = orgModuleRepository.findAllByOrderBySortOrderAscLabelAsc().stream()
                .filter(OrgModule::isEnabled)
                .map(OrgModule::getModuleKey)
                .collect(Collectors.toSet());

        return getMenusFor(role.getId(), role.getName(), effective, enabledModules,
                homeroomGuard.findHomeroomSection().isPresent());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuDto> getMenusFor(Long roleId,
                                     String roleName,
                                     Set<String> effectivePermissions,
                                     Set<String> enabledModules,
                                     boolean holdsHomeroomSection) {

        List<Menu> assigned = menuRepository.findAllByRoleId(roleId);
        if (assigned.isEmpty()) {
            return List.of();
        }

        // SUPER_ADMIN skips the permission gate only. The module gate still applies,
        // because it is a statement about the organisation rather than about their
        // authority - see the class comment.
        boolean bypassesPermissions = AppConstants.ROLE_SUPER_ADMIN.equals(roleName);

        List<Menu> visible = new ArrayList<>();
        for (Menu menu : assigned) {
            if (menu.getModuleKey() != null && !enabledModules.contains(menu.getModuleKey())) {
                continue;
            }
            if (menu.isRequiresHomeroom() && !holdsHomeroomSection) {
                continue;
            }
            if (menu.getRequiredPermission() != null
                    && !bypassesPermissions
                    && !effectivePermissions.contains(menu.getRequiredPermission())) {
                continue;
            }
            visible.add(menu);
        }

        return assembleTree(visible);
    }

    /**
     * Groups the surviving destinations under their headings.
     *
     * <p>A heading only appears when it has a surviving child. A heading with none
     * is not an empty section to render — it is a section this user has no business
     * in, and drawing the title with nothing under it looks like a loading failure.
     *
     * <p>A destination whose heading did not survive its own gates is promoted to
     * the top level rather than dropped. That case should not arise from the seed
     * (headings carry no module or permission), but silently losing a menu because
     * of a misconfigured parent is the worse failure of the two.
     */
    private List<MenuDto> assembleTree(List<Menu> visible) {
        Map<Long, Menu> headingsById = new HashMap<>();
        List<Menu> children = new ArrayList<>();
        for (Menu menu : visible) {
            if (menu.isHeading()) {
                headingsById.put(menu.getId(), menu);
            } else {
                children.add(menu);
            }
        }

        Map<Long, List<MenuDto>> childrenByParent = new HashMap<>();
        List<MenuDto> orphans = new ArrayList<>();
        for (Menu child : children) {
            if (headingsById.containsKey(child.getParentId())) {
                childrenByParent.computeIfAbsent(child.getParentId(), key -> new ArrayList<>())
                        .add(toDto(child, null));
            } else {
                orphans.add(toDto(child, null));
            }
        }

        List<MenuDto> tree = new ArrayList<>();
        // headingsById is populated from findAllByRoleId's ordering, but a HashMap
        // does not preserve it - so walk `visible` again rather than the map.
        for (Menu menu : visible) {
            if (!menu.isHeading()) {
                continue;
            }
            List<MenuDto> kids = childrenByParent.get(menu.getId());
            if (kids == null || kids.isEmpty()) {
                continue;
            }
            tree.add(toDto(menu, kids));
        }
        tree.addAll(orphans);
        return tree;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuDto> getCatalogue() {
        Map<Long, Long> rolesPerMenu = new HashMap<>();
        for (Object[] row : menuRepository.countRolesPerMenu()) {
            rolesPerMenu.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        List<Menu> all = menuRepository.findAllOrdered();

        Map<Long, List<MenuDto>> childrenByParent = new HashMap<>();
        for (Menu menu : all) {
            if (!menu.isHeading()) {
                childrenByParent.computeIfAbsent(menu.getParentId(), key -> new ArrayList<>())
                        .add(withCount(toDto(menu, null), rolesPerMenu));
            }
        }

        // Unfiltered, and headings are kept even when empty: this is the screen
        // where an administrator assigns menus, so it has to show the ones nobody
        // holds yet - those are precisely the rows they are there to change.
        return all.stream()
                .filter(Menu::isHeading)
                .map(heading -> withCount(
                        toDto(heading, childrenByParent.getOrDefault(heading.getId(), List.of())),
                        rolesPerMenu))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getRoleMenuIds(Long roleId) {
        requireRole(roleId);
        return new LinkedHashSet<>(menuRepository.findMenuIdsByRoleId(roleId));
    }

    @Override
    @Transactional
    public Set<Long> replaceRoleMenus(Long roleId, Set<Long> menuIds) {
        Role role = requireRole(roleId);

        if (menuIds == null) {
            throw new BadRequestException("menuIds is required (send an empty array to unassign all)");
        }

        // Validate the whole request before writing any of it, so a typo cannot
        // leave a role holding half a menu with no way to tell which half.
        Set<Long> known = menuRepository.findAllById(menuIds).stream()
                .map(Menu::getId)
                .collect(Collectors.toSet());
        List<Long> unknown = menuIds.stream().filter(id -> !known.contains(id)).sorted().toList();
        if (!unknown.isEmpty()) {
            throw new BadRequestException("Unknown menu id(s): " + unknown.stream()
                    .map(String::valueOf).collect(Collectors.joining(", ")));
        }

        menuRepository.deleteAllByRoleId(roleId);
        if (!known.isEmpty()) {
            // Guarded: MySQL rejects IN () as a syntax error, so an empty desired
            // set has to skip the insert rather than run it with nothing.
            menuRepository.assignToRole(roleId, known);
        }

        log.info("Menus for role {} set to {} entries by {}", role.getName(), known.size(),
                SecurityUtils.getCurrentUserPrincipal().map(UserPrincipal::getUsername).orElse("unknown"));

        return new LinkedHashSet<>(menuRepository.findMenuIdsByRoleId(roleId));
    }

    private Role requireRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
    }

    private MenuDto withCount(MenuDto dto, Map<Long, Long> rolesPerMenu) {
        dto.setAssignedRoleCount(rolesPerMenu.getOrDefault(dto.getId(), 0L));
        return dto;
    }

    private MenuDto toDto(Menu menu, List<MenuDto> children) {
        return MenuDto.builder()
                .id(menu.getId())
                .menuKey(menu.getMenuKey())
                .label(menu.getLabel())
                .path(menu.getPath())
                .icon(menu.getIcon())
                .i18nKey(menu.getI18nKey())
                .sortOrder(menu.getSortOrder())
                .enabled(menu.isEnabled())
                .children(children == null ? List.of() : children)
                .moduleKey(menu.getModuleKey())
                .requiredPermission(menu.getRequiredPermission())
                .requiresHomeroom(menu.isRequiresHomeroom())
                .build();
    }
}
