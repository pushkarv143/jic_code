package com.school.sms.service;

/**
 * Outbound SMS.
 *
 * This project has no SMS gateway — {@code NotificationServiceImpl} has always
 * simulated the channel. The interface exists so the OTP flow is written once
 * against a seam rather than around its absence: swapping in a real provider is
 * a second implementation of this and a configuration change, with nothing in
 * {@code OtpService} to revisit.
 *
 * A provider should be called over its plain REST API using the HTTP client the
 * application already carries, rather than a vendor SDK — the server this runs
 * on has under 300 MB of memory to spare.
 */
public interface SmsService {

    /**
     * @return true when the message was handed to a gateway. The default
     *         implementation returns false, which is what lets the caller tell
     *         "delivered" apart from "pretended to deliver".
     */
    boolean send(String toPhone, String message);

    /** Whether a real gateway is configured. Callers use this to refuse the channel up front. */
    boolean isAvailable();
}
