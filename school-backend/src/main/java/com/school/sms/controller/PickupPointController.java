package com.school.sms.controller;

import com.school.sms.dto.request.PickupPointRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.PickupPointDto;
import com.school.sms.service.PickupPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Separate from RouteController since these two routes live directly under
 * /api/v1/pickup-points/{id} rather than nested under a route — the nested
 * GET/POST for a route's pickup points live on RouteController instead.
 */
@RestController
@RequestMapping("/api/v1/pickup-points")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pickup Points", description = "Manage individual pickup points on a route")
public class PickupPointController {

    private final PickupPointService pickupPointService;

    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a pickup point")
    public ResponseEntity<ApiResponse<PickupPointDto>> update(@PathVariable Long id,
                                                               @Valid @RequestBody PickupPointRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Pickup point updated successfully",
                pickupPointService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete a pickup point")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pickupPointService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
