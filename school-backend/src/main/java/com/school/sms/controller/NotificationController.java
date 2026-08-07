package com.school.sms.controller;

import com.school.sms.dto.request.NotificationSendRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.NotificationDto;
import com.school.sms.dto.response.NotificationSendResultDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "In-app/SMS/email/push notifications")
public class NotificationController {

    private final NotificationService notificationService;

    // SMS/PUSH have no real gateway integration in this project — those rows
    // are persisted and marked SENT as a simulation, documented on NotificationServiceImpl.
    private static final String SEND_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "The current user's own notifications, newest first")
    public ResponseEntity<ApiResponse<PageResponse<NotificationDto>>> getMy(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", notificationService.getMy(pageable)));
    }

    @PostMapping("/send")
    @PreAuthorize(SEND_ROLES)
    @Operation(summary = "Send a notification to a single recipient or fan it out to every active user of a role "
            + "(exactly one of recipientId/targetRole); EMAIL also triggers a real email via EmailService")
    public ResponseEntity<ApiResponse<NotificationSendResultDto>> send(@Valid @RequestBody NotificationSendRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Notification sent successfully", notificationService.send(request)));
    }
}
