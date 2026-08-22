package com.school.sms.controller;

import com.school.sms.dto.request.FeeCategoryRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.FeeCategoryDto;
import com.school.sms.service.FeeCategoryService;
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
@RequestMapping("/api/v1/fee-categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fee Categories", description = "Manage fee categories (tuition, transport, library...)")
public class FeeCategoryController {

    private final FeeCategoryService feeCategoryService;

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','ACCOUNTANT','TEACHER','RECEPTIONIST')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','ACCOUNTANT')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List all fee categories")
    public ResponseEntity<ApiResponse<List<FeeCategoryDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Fee categories retrieved successfully", feeCategoryService.getAll()));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create a new fee category")
    public ResponseEntity<ApiResponse<FeeCategoryDto>> create(@Valid @RequestBody FeeCategoryRequest request) {
        FeeCategoryDto created = feeCategoryService.create(request);
        URI location = URI.create("/api/v1/fee-categories/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Fee category created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a fee category")
    public ResponseEntity<ApiResponse<FeeCategoryDto>> update(@PathVariable Long id,
                                                               @Valid @RequestBody FeeCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Fee category updated successfully", feeCategoryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete a fee category")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        feeCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
