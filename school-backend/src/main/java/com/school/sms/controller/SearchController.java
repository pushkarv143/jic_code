package com.school.sms.controller;

import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.GlobalSearchResultDto;
import com.school.sms.service.GlobalSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Global Search", description = "Cross-module quick search across students, teachers and books")
public class SearchController {

    private final GlobalSearchService globalSearchService;

    @GetMapping("/global")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search students/teachers/books by a free-text query, up to 5 results per category")
    public ResponseEntity<ApiResponse<GlobalSearchResultDto>> globalSearch(@RequestParam String query) {
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully",
                globalSearchService.search(query)));
    }
}
