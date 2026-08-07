package com.school.sms.controller;

import com.school.sms.dto.request.EventRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.EventDto;
import com.school.sms.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Events", description = "School calendar events (holidays, exams, other events)")
public class EventController {

    private final EventService eventService;

    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List calendar events in a date range, optionally filtered by event type")
    public ResponseEntity<ApiResponse<List<EventDto>>> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String eventType) {
        return ResponseEntity.ok(ApiResponse.success("Events retrieved successfully",
                eventService.getAll(startDate, endDate, eventType)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create a calendar event")
    public ResponseEntity<ApiResponse<EventDto>> create(@Valid @RequestBody EventRequest request) {
        EventDto created = eventService.create(request);
        URI location = URI.create("/api/v1/events/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Event created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a calendar event")
    public ResponseEntity<ApiResponse<EventDto>> update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Event updated successfully", eventService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete a calendar event")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
