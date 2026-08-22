package com.school.sms.controller;

import com.school.sms.dto.request.UpdateOrgModulesRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.OrgModuleDto;
import com.school.sms.service.AccessService;
import com.school.sms.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The module registry — which functional areas this organisation runs.
 *
 * <p>Switching a module off withdraws every permission in it from every role at
 * once, which is the coarse dial an organisation reaches for when it simply does
 * not use hostels or transport. Fine-grained per-role tuning lives on
 * {@link RoleController} instead.
 */
@RestController
@RequestMapping("/api/v1/org/modules")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Org Modules", description = "Enable or disable whole functional modules for this organisation")
public class OrgModuleController {

    private final AccessService accessService;

    private static final String READ =
            "hasAuthority('" + AppConstants.PERMISSION_AUTHORITY_PREFIX + "SETTINGS_VIEW') or "
                    + AppConstants.ADMIN_OVERRIDE;

    private static final String MANAGE =
            "hasAuthority('" + AppConstants.PERMISSION_AUTHORITY_PREFIX + "SETTINGS_MANAGE') or "
                    + AppConstants.ADMIN_OVERRIDE;

    @GetMapping
    @PreAuthorize(READ)
    @Operation(summary = "List every module with its enabled state and permission count")
    public ResponseEntity<ApiResponse<List<OrgModuleDto>>> getModules() {
        return ResponseEntity.ok(ApiResponse.success("Modules retrieved successfully", accessService.getModules()));
    }

    /**
     * Toggles modules. Core modules are refused — see {@code OrgModule#core} for why
     * the installation must keep a way back.
     */
    @PutMapping
    @PreAuthorize(MANAGE)
    @Operation(summary = "Enable or disable modules by key (core modules cannot be disabled)")
    public ResponseEntity<ApiResponse<List<OrgModuleDto>>> updateModules(
            @Valid @RequestBody UpdateOrgModulesRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Modules updated successfully",
                accessService.updateModules(request.getModules())));
    }
}
