package com.school.sms.service;

public interface EmailService {

    void sendPasswordResetEmail(String to, String recipientName, String resetLink);

    void sendWelcomeEmail(String to, String name, String username);

    /**
     * Generic subject/body email used by the notifications module
     * (POST /api/v1/notifications/send with type=EMAIL) — reuses the same
     * JavaMailSender plumbing as the password-reset/welcome templates instead
     * of standing up a second email mechanism.
     */
    void sendGenericNotification(String to, String subject, String message);
}
