package com.school.sms.controller;

import com.school.sms.dto.request.ReplaceRoleMenusRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.MenuDto;
import com.school.sms.service.MenuService;
import com.school.sms.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * The navigation menu: what this user sees, and who is assigned what.
 *
 * <p>Two audiences, two authorization rules. {@code /me/menus} is the caller's own
 * menu and needs nothing but a login — refusing a user their own menu would leave
 * them staring at an empty shell. The catalogue and the per-role assignment are
 * administration, gated on the same {@code ROLE_MANAGE} grant as the permission
 * editor, because assigning menus and granting permissions are two halves of the
 * same job and splitting them across two grants only invites one to be forgotten.
 *
 * @see com.school.sms.entity.Menu for why a menu assignment is not authority
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Menus", description = "Role-based navigation menus")
public class MenuController {

    private final MenuService menuService;

    private static final String READ_CONFIG =
            "hasAuthority('" + AppConstants.PERMISSION_AUTHORITY_PREFIX + "ROLE_READ') or "
                    + AppConstants.ADMIN_OVERRIDE;

    private static final String MANAGE_CONFIG =
            "hasAuthority('" + AppConstants.PERMISSION_AUTHORITY_PREFIX + "ROLE_MANAGE') or "
                    + AppConstants.ADMIN_OVERRIDE;

    /**
     * The signed-in user's menu, already filtered.
     *
     * <p>Also returned inside {@code GET /me/access}, which is what the clients
     * actually call on start-up — one round trip for grants, modules, homeroom and
     * menu together. This endpoint exists for a client that wants to refresh the
     * menu alone, and to make the filter directly testable.
     */
    @GetMapping("/me/menus")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "The signed-in user's navigation menu, as a tree of sections")
    public ResponseEntity<ApiResponse<List<MenuDto>>> getMyMenus() {
        return ResponseEntity.ok(ApiResponse.success("Menus retrieved successfully", menuService.getMyMenus()));
    }

    @GetMapping("/menus")
    @PreAuthorize(READ_CONFIG)
    @Operation(summary = "Every menu, unfiltered, with how many roles hold each")
    public ResponseEntity<ApiResponse<List<MenuDto>>> getCatalogue() {
        return ResponseEntity.ok(ApiResponse.success("Menu catalogue retrieved successfully",
                menuService.getCatalogue()));
    }

    @GetMapping("/roles/{roleId}/menus")
    @PreAuthorize(READ_CONFIG)
    @Operation(summary = "Menu ids assigned to one role")
    public ResponseEntity<ApiResponse<Set<Long>>> getRoleMenus(@PathVariable Long roleId) {
        return ResponseEntity.ok(ApiResponse.success("Role menus retrieved successfully",
                menuService.getRoleMenuIds(roleId)));
    }

    /**
     * Replaces one role's menu assignment.
     *
     * <p>Absolute, not a delta: anything missing from the request is unassigned.
     * An empty array is accepted and means this role sees no menu.
     */
    @PutMapping("/roles/{roleId}/menus")
    @PreAuthorize(MANAGE_CONFIG)
    @Operation(summary = "Set which menus a role is assigned (replaces the whole set)")
    public ResponseEntity<ApiResponse<Set<Long>>> replaceRoleMenus(
            @PathVariable Long roleId,
            @Valid @RequestBody ReplaceRoleMenusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Role menus updated successfully",
                menuService.replaceRoleMenus(roleId, request.getMenuIds())));
    }
}
