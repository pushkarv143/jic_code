package com.school.sms.controller;

import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.MyAccessDto;
import com.school.sms.service.AccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * What the signed-in user may do — the single call the frontend shell makes before
 * it decides which menu entries and page actions to render.
 */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "My Access", description = "Effective permissions, enabled modules and homeroom for the caller")
public class AccessController {

    private final AccessService accessService;

    /**
     * Open to every authenticated caller, and it has to be: this is how a session
     * learns what it is allowed to do, so gating it behind a permission would be
     * circular. It returns only facts about the caller themselves and never
     * another user's grants.
     */
    @GetMapping("/access")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Effective role, permissions, enabled modules and homeroom section for the caller")
    public ResponseEntity<ApiResponse<MyAccessDto>> getMyAccess() {
        return ResponseEntity.ok(ApiResponse.success("Access retrieved successfully", accessService.getMyAccess()));
    }
}
