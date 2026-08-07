package com.school.sms.controller;

import com.school.sms.dto.request.SalaryStructureRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.SalaryStructureDto;
import com.school.sms.entity.PayrollEmployeeType;
import com.school.sms.service.SalaryStructureService;
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
@RequestMapping("/api/v1/salary-structures")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Salary Structures", description = "Per-employee payroll salary structure (basic/hra/da/allowances/pf/esi)")
public class SalaryStructureController {

    private final SalaryStructureService salaryStructureService;

    private static final String READ_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','ACCOUNTANT')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','ACCOUNTANT')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List salary structures, optionally filtered by employee id (users.id) / employee type")
    public ResponseEntity<ApiResponse<List<SalaryStructureDto>>> getAll(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) PayrollEmployeeType employeeType) {
        return ResponseEntity.ok(ApiResponse.success("Salary structures retrieved successfully",
                salaryStructureService.getAll(employeeId, employeeType)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create a salary structure for an employee (one per employee_id, per schema)")
    public ResponseEntity<ApiResponse<SalaryStructureDto>> create(@Valid @RequestBody SalaryStructureRequest request) {
        SalaryStructureDto created = salaryStructureService.create(request);
        URI location = URI.create("/api/v1/salary-structures/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Salary structure created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a salary structure")
    public ResponseEntity<ApiResponse<SalaryStructureDto>> update(@PathVariable Long id,
                                                                   @Valid @RequestBody SalaryStructureRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Salary structure updated successfully",
                salaryStructureService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete a salary structure")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salaryStructureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
