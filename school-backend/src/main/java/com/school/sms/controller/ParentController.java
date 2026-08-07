package com.school.sms.controller;

import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.ChildDto;
import com.school.sms.service.ParentPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parents")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Parent Portal", description = "Self-service endpoints for the authenticated PARENT user")
public class ParentController {

    private final ParentPortalService parentPortalService;

    @GetMapping("/me/children")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "List the authenticated parent's own children")
    public ResponseEntity<ApiResponse<List<ChildDto>>> getMyChildren() {
        return ResponseEntity.ok(ApiResponse.success("Children retrieved successfully", parentPortalService.getMyChildren()));
    }
}
