package com.school.sms.service.impl;

import com.school.sms.dto.request.NotificationSendRequest;
import com.school.sms.dto.response.NotificationDto;
import com.school.sms.dto.response.NotificationSendResultDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.entity.Notification;
import com.school.sms.entity.NotificationStatus;
import com.school.sms.entity.NotificationType;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.NotificationRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.security.SecurityUtils;
import com.school.sms.service.EmailService;
import com.school.sms.service.NotificationService;
import com.school.sms.util.NameUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> getMy(Pageable pageable) {
        Page<Notification> page = notificationRepository
                .findAllByRecipientIdOrderByIdDesc(SecurityUtils.getCurrentUserId(), pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toDto).toList());
    }

    @Override
    @Transactional
    public NotificationSendResultDto send(NotificationSendRequest request) {
        boolean hasRecipient = request.getRecipientId() != null;
        boolean hasTargetRole = StringUtils.hasText(request.getTargetRole());
        if (hasRecipient == hasTargetRole) {
            throw new BadRequestException("Exactly one of recipientId or targetRole must be provided");
        }

        List<User> recipients;
        if (hasRecipient) {
            User recipient = userRepository.findById(request.getRecipientId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getRecipientId()));
            recipients = List.of(recipient);
        } else {
            recipients = userRepository.findAllByRole_NameAndActiveTrue(request.getTargetRole().toUpperCase());
            if (recipients.isEmpty()) {
                throw new ResourceNotFoundException("No active users found for role", "targetRole", request.getTargetRole());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<Notification> toSave = recipients.stream()
                .map(recipient -> {
                    // SMS/PUSH have no real gateway integration in this project (simulated,
                    // marked SENT immediately); EMAIL actually calls EmailService below.
                    Notification notification = Notification.builder()
                            .recipientId(recipient.getId())
                            .type(request.getType())
                            .subject(request.getSubject())
                            .message(request.getMessage())
                            .status(NotificationStatus.SENT)
                            .sentAt(now)
                            .build();

                    if (request.getType() == NotificationType.EMAIL) {
                        emailService.sendGenericNotification(
                                recipient.getEmail(),
                                StringUtils.hasText(request.getSubject()) ? request.getSubject() : "Notification",
                                request.getMessage());
                    }
                    return notification;
                })
                .toList();

        notificationRepository.saveAll(toSave);

        return NotificationSendResultDto.builder().recipientCount(recipients.size()).build();
    }

    private NotificationDto toDto(Notification notification) {
        User recipient = userRepository.findById(notification.getRecipientId()).orElse(null);
        return NotificationDto.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipientId())
                .recipientName(recipient != null ? NameUtil.fullName(recipient.getFirstName(), recipient.getLastName()) : null)
                .type(notification.getType().name())
                .subject(notification.getSubject())
                .message(notification.getMessage())
                .status(notification.getStatus().name())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
