package com.school.sms.controller;

import com.school.sms.dto.request.FeeStructureRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.FeeStructureDto;
import com.school.sms.service.FeeStructureService;
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
@RequestMapping("/api/v1/fee-structures")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fee Structures", description = "Manage per-class, per-year fee structures")
public class FeeStructureController {

    private final FeeStructureService feeStructureService;

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','ACCOUNTANT','TEACHER','CLASS_TEACHER','RECEPTIONIST')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','ACCOUNTANT')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List fee structures, optionally filtered by class/academic year/fee category")
    public ResponseEntity<ApiResponse<List<FeeStructureDto>>> getAll(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long feeCategoryId) {
        return ResponseEntity.ok(ApiResponse.success("Fee structures retrieved successfully",
                feeStructureService.getAll(classId, academicYearId, feeCategoryId)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create a new fee structure for a class/academic year/fee category")
    public ResponseEntity<ApiResponse<FeeStructureDto>> create(@Valid @RequestBody FeeStructureRequest request) {
        FeeStructureDto created = feeStructureService.create(request);
        URI location = URI.create("/api/v1/fee-structures/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Fee structure created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a fee structure")
    public ResponseEntity<ApiResponse<FeeStructureDto>> update(@PathVariable Long id,
                                                                @Valid @RequestBody FeeStructureRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Fee structure updated successfully",
                feeStructureService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete a fee structure")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        feeStructureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
