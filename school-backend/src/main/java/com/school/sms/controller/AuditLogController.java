package com.school.sms.controller;

import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.AuditLogDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Audit Logs", description = "Read-only trail of high-value actions (logins, user/student CRUD, fee payments, payroll, leave decisions)")
public class AuditLogController {

    private final AuditLogService auditLogService;

    private static final String READ_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Paginated audit log, filterable by user/entity/action/date range")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDto>>> getAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully",
                auditLogService.getAll(userId, entityName, action, startDate, endDate, pageable)));
    }
}
