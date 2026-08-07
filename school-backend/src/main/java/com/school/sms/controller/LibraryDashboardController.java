package com.school.sms.controller;

import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.LibraryDashboardDto;
import com.school.sms.service.LibraryDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/library")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Library Dashboard", description = "Aggregate library figures for dashboard cards")
public class LibraryDashboardController {

    private final LibraryDashboardService libraryDashboardService;

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','LIBRARIAN')";

    @GetMapping("/dashboard")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Aggregate total books/copies, available copies, issued and overdue counts")
    public ResponseEntity<ApiResponse<LibraryDashboardDto>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Library dashboard retrieved successfully",
                libraryDashboardService.getDashboard()));
    }
}
