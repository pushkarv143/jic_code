package com.school.sms.dto.request;

import com.school.sms.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Exactly one of recipientId/targetRole must be set — enforced in
 * NotificationServiceImpl (a cross-field XOR isn't worth a custom bean
 * validation annotation for a single request DTO; matches how similarly
 * shaped cross-field rules are checked in service code elsewhere, e.g.
 * LeaveApplicationServiceImpl's date-range check).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSendRequest {

    private Long recipientId;

    private String targetRole;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @Size(max = 255, message = "Subject must not exceed 255 characters")
    private String subject;

    @NotBlank(message = "Message is required")
    private String message;
}
