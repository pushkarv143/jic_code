package com.school.sms.service.impl;

import com.school.sms.dto.response.AuditLogDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.entity.AuditLog;
import com.school.sms.entity.User;
import com.school.sms.repository.AuditLogRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.security.SecurityUtils;
import com.school.sms.security.UserPrincipal;
import com.school.sms.service.AuditLogService;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void record(String action, String entityName, Long entityId, String oldValue, String newValue) {
        try {
            User user = SecurityUtils.getCurrentUserPrincipal()
                    .map(UserPrincipal::getId)
                    .flatMap(userRepository::findById)
                    .orElse(null);

            AuditLog entry = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .entityName(entityName)
                    .entityId(entityId)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .ipAddress(resolveClientIp())
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            // Fire-and-forget: an audit-logging failure must never break the caller's
            // own business transaction (login, payment, approval, ...).
            log.warn("Failed to record audit log entry (action={}, entity={}/{}): {}",
                    action, entityName, entityId, ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogDto> getAll(Long userId, String entityName, String action,
                                             LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? LocalDateTime.of(endDate, LocalTime.MAX) : null;

        Specification<AuditLog> spec = new SpecificationBuilder<AuditLog>()
                .with(userId != null, "user.id", SearchOperation.EQUALS, userId)
                .with(StringUtils.hasText(entityName), "entityName", SearchOperation.EQUALS, entityName)
                .with(StringUtils.hasText(action), "action", SearchOperation.EQUALS, action)
                .with(startDateTime != null, "createdAt", SearchOperation.GREATER_THAN_EQUAL, startDateTime)
                .with(endDateTime != null, "createdAt", SearchOperation.LESS_THAN_EQUAL, endDateTime)
                .build();

        Page<AuditLog> page = auditLogRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toDto).toList());
    }

    private String resolveClientIp() {
        try {
            if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(forwardedFor)) {
                return forwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception ex) {
            return null;
        }
    }

    private AuditLogDto toDto(AuditLog entry) {
        User user = entry.getUser();
        return AuditLogDto.builder()
                .id(entry.getId())
                .userId(user != null ? user.getId() : null)
                .userName(user != null ? NameUtil.fullName(user.getFirstName(), user.getLastName()) : null)
                .action(entry.getAction())
                .entityName(entry.getEntityName())
                .entityId(entry.getEntityId())
                .oldValue(entry.getOldValue())
                .newValue(entry.getNewValue())
                .ipAddress(entry.getIpAddress())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
