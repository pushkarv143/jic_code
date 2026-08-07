package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {

    private Long id;
    private Long recipientId;
    private String recipientName;
    private String type;
    private String subject;
    private String message;
    private String status;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
