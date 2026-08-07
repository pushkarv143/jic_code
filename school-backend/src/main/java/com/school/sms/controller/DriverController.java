package com.school.sms.controller;

import com.school.sms.dto.request.DriverRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.DriverDto;
import com.school.sms.service.DriverService;
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
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Drivers", description = "Manage transport drivers")
public class DriverController {

    private final DriverService driverService;

    private static final String READ_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','RECEPTIONIST')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List all drivers")
    public ResponseEntity<ApiResponse<List<DriverDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Drivers retrieved successfully", driverService.getAll()));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Add a new driver")
    public ResponseEntity<ApiResponse<DriverDto>> create(@Valid @RequestBody DriverRequest request) {
        DriverDto created = driverService.create(request);
        URI location = URI.create("/api/v1/drivers/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Driver created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a driver")
    public ResponseEntity<ApiResponse<DriverDto>> update(@PathVariable Long id, @Valid @RequestBody DriverRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Driver updated successfully", driverService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete a driver")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        driverService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
