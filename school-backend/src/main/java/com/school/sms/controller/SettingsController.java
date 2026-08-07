package com.school.sms.controller;

import com.school.sms.dto.request.SchoolInfoRequest;
import com.school.sms.dto.request.SystemSettingRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.SchoolInfoDto;
import com.school.sms.dto.response.SystemSettingDto;
import com.school.sms.service.BackupService;
import com.school.sms.service.SchoolInfoService;
import com.school.sms.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Settings", description = "School profile info, global system settings and reference-data backup")
public class SettingsController {

    private final SchoolInfoService schoolInfoService;
    private final SystemSettingService systemSettingService;
    private final BackupService backupService;

    private static final String READ_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL')";
    private static final String WRITE_ROLES = "hasRole('SUPER_ADMIN')";

    @GetMapping("/school-info")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Get the school profile info; creates a default (empty) singleton row on first read")
    public ResponseEntity<ApiResponse<SchoolInfoDto>> getSchoolInfo() {
        return ResponseEntity.ok(ApiResponse.success("School info retrieved successfully", schoolInfoService.get()));
    }

    @PutMapping("/school-info")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update the school profile info")
    public ResponseEntity<ApiResponse<SchoolInfoDto>> updateSchoolInfo(@Valid @RequestBody SchoolInfoRequest request) {
        return ResponseEntity.ok(ApiResponse.success("School info updated successfully", schoolInfoService.update(request)));
    }

    @GetMapping("/system")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List all system settings as key/value pairs")
    public ResponseEntity<ApiResponse<List<SystemSettingDto>>> getSystemSettings() {
        return ResponseEntity.ok(ApiResponse.success("System settings retrieved successfully", systemSettingService.getAll()));
    }

    @PutMapping("/system")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Upsert one or more system settings by key")
    public ResponseEntity<ApiResponse<List<SystemSettingDto>>> updateSystemSettings(
            @Valid @RequestBody List<@Valid SystemSettingRequest> requests) {
        return ResponseEntity.ok(ApiResponse.success("System settings updated successfully",
                systemSettingService.upsertAll(requests)));
    }

    @GetMapping("/backup/export")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Download a JSON backup of reference/config tables only (school info, settings, roles, "
            + "departments, designations, academic years, fee categories, exam types, grades) — not a full DB dump")
    public ResponseEntity<byte[]> exportBackup() {
        byte[] json = backupService.exportReferenceData();
        String filename = "backup-reference-data-" + LocalDate.now() + ".json";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(json);
    }
}
