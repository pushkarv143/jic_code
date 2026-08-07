package com.school.sms.controller;

import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.PermissionDto;
import com.school.sms.dto.response.RoleDto;
import com.school.sms.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    private static final String READ_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List all roles")
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", roleService.getAll()));
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List the permissions granted to a role via role_permissions")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getPermissions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Permissions retrieved successfully", roleService.getPermissions(id)));
    }
}
