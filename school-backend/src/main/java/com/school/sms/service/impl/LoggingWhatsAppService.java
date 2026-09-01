package com.school.sms.service.impl;

import com.school.sms.service.WhatsAppService;
import lombok.extern.slf4j.Slf4j;

/**
 * No-op WhatsApp service used when Twilio WhatsApp is not configured.
 *
 * <p>Returns {@code false} from every send so callers can distinguish
 * "not configured" from "delivered" without a null check.
 */
@Slf4j
public class LoggingWhatsAppService implements WhatsAppService {

    @Override
    public boolean send(String toPhone, String message) {
        log.debug("[WhatsApp stub] would send to {}: {}", mask(toPhone), message);
        return false;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}
