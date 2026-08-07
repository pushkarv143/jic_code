package com.school.sms.service;

import com.school.sms.dto.response.AuditLogDto;
import com.school.sms.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AuditLogService {

    /**
     * Fire-and-forget: records one audit trail entry (current user + IP resolved
     * internally via SecurityUtils/RequestContextHolder). Never throws — a failure
     * to write the audit row must never break the caller's own transaction, so
     * every exception is caught and logged internally instead of propagated.
     */
    void record(String action, String entityName, Long entityId, String oldValue, String newValue);

    PageResponse<AuditLogDto> getAll(Long userId, String entityName, String action,
                                      LocalDate startDate, LocalDate endDate, Pageable pageable);
}
