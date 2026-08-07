package com.school.sms.controller;

import com.school.sms.dto.request.HostelRoomRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.HostelRoomDto;
import com.school.sms.service.HostelRoomService;
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
 * Separate from HostelController since these two routes live directly under
 * /api/v1/hostel-rooms/{id} rather than nested under a hostel — the nested
 * GET/POST for a hostel's rooms live on HostelController instead.
 */
@RestController
@RequestMapping("/api/v1/hostel-rooms")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Hostel Rooms", description = "Manage individual hostel rooms")
public class HostelRoomController {

    private final HostelRoomService hostelRoomService;

    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a hostel room")
    public ResponseEntity<ApiResponse<HostelRoomDto>> update(@PathVariable Long id,
                                                              @Valid @RequestBody HostelRoomRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Hostel room updated successfully",
                hostelRoomService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete a hostel room")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        hostelRoomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
