package com.school.sms.service.impl;

import com.school.sms.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * The stand-in used until a gateway is configured.
 *
 * It reports itself unavailable rather than quietly succeeding, so a request to
 * send an OTP by SMS is refused with a clear message instead of leaving someone
 * waiting for a text that was never going to arrive.
 *
 * {@code @ConditionalOnMissingBean} means adding a real implementation replaces
 * this one automatically, with nothing to unregister.
 */
@Slf4j
@Service
@ConditionalOnMissingBean(name = "smsGateway")
public class LoggingSmsServiceImpl implements SmsService {

    @Override
    public boolean send(String toPhone, String message) {
        // The message body is not logged. For an OTP it would be the code itself,
        // and application logs are the easiest place in the system to read.
        log.warn("SMS requested for {} but no gateway is configured; nothing was sent", maskPhone(toPhone));
        return false;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    /** Keeps enough of the number to correlate against a support call, not enough to be a contact list. */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }
}
