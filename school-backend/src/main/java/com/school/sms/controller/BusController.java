package com.school.sms.controller;

import com.school.sms.dto.request.BusRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.BusDto;
import com.school.sms.service.BusService;
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
@RequestMapping("/api/v1/buses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Buses", description = "Manage the transport fleet")
public class BusController {

    private final BusService busService;

    private static final String READ_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','RECEPTIONIST')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List all buses")
    public ResponseEntity<ApiResponse<List<BusDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Buses retrieved successfully", busService.getAll()));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Add a new bus")
    public ResponseEntity<ApiResponse<BusDto>> create(@Valid @RequestBody BusRequest request) {
        BusDto created = busService.create(request);
        URI location = URI.create("/api/v1/buses/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Bus created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a bus")
    public ResponseEntity<ApiResponse<BusDto>> update(@PathVariable Long id, @Valid @RequestBody BusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bus updated successfully", busService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Soft-delete a bus")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        busService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
