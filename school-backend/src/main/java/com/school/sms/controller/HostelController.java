package com.school.sms.controller;

import com.school.sms.dto.request.HostelRequest;
import com.school.sms.dto.request.HostelRoomRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.HostelDto;
import com.school.sms.dto.response.HostelRoomDto;
import com.school.sms.service.HostelRoomService;
import com.school.sms.service.HostelService;
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
@RequestMapping("/api/v1/hostels")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Hostels", description = "Manage hostels and their rooms")
public class HostelController {

    private final HostelService hostelService;
    private final HostelRoomService hostelRoomService;

    private static final String READ_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List all hostels")
    public ResponseEntity<ApiResponse<List<HostelDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Hostels retrieved successfully", hostelService.getAll()));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create a new hostel")
    public ResponseEntity<ApiResponse<HostelDto>> create(@Valid @RequestBody HostelRequest request) {
        HostelDto created = hostelService.create(request);
        URI location = URI.create("/api/v1/hostels/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Hostel created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a hostel")
    public ResponseEntity<ApiResponse<HostelDto>> update(@PathVariable Long id, @Valid @RequestBody HostelRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Hostel updated successfully", hostelService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete a hostel")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        hostelService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{hostelId}/rooms")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List a hostel's rooms")
    public ResponseEntity<ApiResponse<List<HostelRoomDto>>> getRooms(@PathVariable Long hostelId) {
        return ResponseEntity.ok(ApiResponse.success("Hostel rooms retrieved successfully",
                hostelRoomService.getByHostel(hostelId)));
    }

    @PostMapping("/{hostelId}/rooms")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Add a room to a hostel")
    public ResponseEntity<ApiResponse<HostelRoomDto>> addRoom(@PathVariable Long hostelId,
                                                               @Valid @RequestBody HostelRoomRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Hostel room created successfully",
                hostelRoomService.create(hostelId, request)));
    }
}
