package com.school.sms.service;

/**
 * Outbound WhatsApp messaging via Twilio's WhatsApp channel.
 *
 * <p>Structurally identical to {@link SmsService}: the same Twilio account handles
 * both SMS and WhatsApp — the only difference is a {@code whatsapp:} prefix on the
 * sender and recipient numbers.  A stub ({@link LoggingWhatsAppService}) is wired
 * when the channel is not configured so callers never have to null-check.
 *
 * <p>To enable the real channel, set in the environment:
 * <pre>
 *   TWILIO_ACCOUNT_SID   (shared with SMS)
 *   TWILIO_AUTH_TOKEN    (shared with SMS)
 *   TWILIO_WHATSAPP_FROM (the approved WhatsApp sender, e.g. whatsapp:+14155238886)
 * </pre>
 */
public interface WhatsAppService {

    /**
     * Sends a WhatsApp message.
     *
     * @param toPhone recipient phone in any reasonable format; normalised to E.164
     *                with the country-code default before dispatch.
     * @param message the message body (plain text; emoji are fine).
     * @return {@code true} if the message was handed off to Twilio.
     */
    boolean send(String toPhone, String message);

    /** {@code false} when the channel is not configured; callers use this to skip gracefully. */
    boolean isAvailable();
}
