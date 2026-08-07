package com.school.sms.controller;

import com.school.sms.dto.request.PickupPointRequest;
import com.school.sms.dto.request.RouteRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.PickupPointDto;
import com.school.sms.dto.response.RouteDto;
import com.school.sms.service.PickupPointService;
import com.school.sms.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Routes", description = "Manage transport routes and their pickup points")
public class RouteController {

    private final RouteService routeService;
    private final PickupPointService pickupPointService;

    private static final String READ_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','RECEPTIONIST')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List routes, optionally filtered by bus")
    public ResponseEntity<ApiResponse<List<RouteDto>>> getAll(@RequestParam(required = false) Long busId) {
        return ResponseEntity.ok(ApiResponse.success("Routes retrieved successfully", routeService.getAll(busId)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create a new route")
    public ResponseEntity<ApiResponse<RouteDto>> create(@Valid @RequestBody RouteRequest request) {
        RouteDto created = routeService.create(request);
        URI location = URI.create("/api/v1/routes/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Route created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a route")
    public ResponseEntity<ApiResponse<RouteDto>> update(@PathVariable Long id, @Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Route updated successfully", routeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete a route")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        routeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{routeId}/pickup-points")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List a route's pickup points")
    public ResponseEntity<ApiResponse<List<PickupPointDto>>> getPickupPoints(@PathVariable Long routeId) {
        return ResponseEntity.ok(ApiResponse.success("Pickup points retrieved successfully",
                pickupPointService.getByRoute(routeId)));
    }

    @PostMapping("/{routeId}/pickup-points")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Add a pickup point to a route")
    public ResponseEntity<ApiResponse<PickupPointDto>> addPickupPoint(@PathVariable Long routeId,
                                                                       @Valid @RequestBody PickupPointRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Pickup point created successfully",
                pickupPointService.create(routeId, request)));
    }
}
