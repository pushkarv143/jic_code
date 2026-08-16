package com.school.sms.service.impl;

import com.school.sms.service.SmsService;
import lombok.extern.slf4j.Slf4j;

/**
 * The stand-in used until a gateway is configured.
 *
 * It reports itself unavailable rather than quietly succeeding, so a request to
 * send an OTP by SMS is refused with a clear message instead of leaving someone
 * waiting for a text that was never going to arrive.
 *
 * Instantiated by {@code SmsConfig}, which decides between this and the real
 * gateway. It used to carry {@code @ConditionalOnMissingBean(name = "smsGateway")}
 * — a bean name nothing ever defined, so the condition always held and this stub
 * would have kept winning even after a gateway was added.
 */
@Slf4j
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
