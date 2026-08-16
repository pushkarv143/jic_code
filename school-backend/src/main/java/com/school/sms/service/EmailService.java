package com.school.sms.service;

public interface EmailService {

    void sendPasswordResetEmail(String to, String recipientName, String resetLink);

    /**
     * Sends a one-time passcode. Separate from the reset-link email because the
     * message has to read differently — a code the reader retypes, with the
     * expiry stated plainly and no link to click, since a link in a "someone is
     * resetting your password" email is exactly what phishing imitates.
     *
     * @param minutesValid stated in the body so the reader knows how long they have
     */
    void sendOtpEmail(String to, String recipientName, String code, int minutesValid, String purposeText);

    void sendWelcomeEmail(String to, String name, String username);

    /**
     * Generic subject/body email used by the notifications module
     * (POST /api/v1/notifications/send with type=EMAIL) — reuses the same
     * JavaMailSender plumbing as the password-reset/welcome templates instead
     * of standing up a second email mechanism.
     */
    void sendGenericNotification(String to, String subject, String message);
}
