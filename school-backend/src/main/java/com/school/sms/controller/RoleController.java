package com.school.sms.controller;

import com.school.sms.dto.request.ReplaceRolePermissionsRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.PermissionDto;
import com.school.sms.dto.response.RoleDto;
import com.school.sms.service.RoleService;
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

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Roles", description = "The fixed set of seeded roles and their granted permissions")
public class RoleController {

    private final RoleService roleService;

    /**
     * Gated on the permission rather than a role list.
     *
     * <p>This was {@code hasAnyRole('SUPER_ADMIN','PRINCIPAL')}, which made the
     * grants readable by exactly two roles no matter what {@code role_permissions}
     * said — so an organisation that wanted, say, a VICE_PRINCIPAL to audit roles
     * could grant ROLE_VIEW and still be refused. Reading the permission instead is
     * what lets the role editor below actually change anything that matters.
     */
    private static final String READ =
            "hasAuthority('" + AppConstants.PERMISSION_AUTHORITY_PREFIX + "ROLE_VIEW') or "
                    + AppConstants.ADMIN_OVERRIDE;

    private static final String MANAGE =
            "hasAuthority('" + AppConstants.PERMISSION_AUTHORITY_PREFIX + "ROLE_MANAGE') or "
                    + AppConstants.ADMIN_OVERRIDE;

    @GetMapping
    @PreAuthorize(READ)
    @Operation(summary = "List all roles")
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", roleService.getAll()));
    }

    @GetMapping("/permission-catalogue")
    @PreAuthorize(READ)
    @Operation(summary = "List every permission that exists, for the role editor's checkbox board")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getPermissionCatalogue() {
        return ResponseEntity.ok(
                ApiResponse.success("Permission catalogue retrieved successfully", roleService.getPermissionCatalogue()));
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize(READ)
    @Operation(summary = "List the permissions granted to a role via role_permissions")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getPermissions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Permissions retrieved successfully", roleService.getPermissions(id)));
    }

    /**
     * Replaces a role's grants with the supplied set.
     *
     * <p>Takes effect for a signed-in user on their next {@code GET /api/v1/me/access}
     * — the app fetches that on start and after any change here — but NOT for the
     * {@code PERM_*} authorities already baked into their current JWT. Those are
     * rebuilt from {@code role_permissions} on each request by
     * {@code CustomUserDetailsService}, so API enforcement is current immediately;
     * it is only a long-lived UI session that can briefly show a stale menu.
     */
    @PutMapping("/{id}/permissions")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Replace a role's permission grants (SUPER_ADMIN is not editable)")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> replacePermissions(
            @PathVariable Long id,
            @Valid @RequestBody ReplaceRolePermissionsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Role permissions updated successfully",
                roleService.replacePermissions(id, request.getPermissions())));
    }
}
