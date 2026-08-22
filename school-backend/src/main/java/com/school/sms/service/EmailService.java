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
     * The username and first-time password for an account the school just created.
     *
     * <p>Separate from {@link #sendWelcomeEmail}, which greets an account whose
     * owner already knows their password. This one carries a secret, so it says
     * plainly that the password is temporary and must be changed at first sign-in —
     * a credentials email that reads like a welcome invites the reader to file it
     * and keep using what it contains.
     *
     * <p>The password is passed in, used once and never persisted in clear: it
     * exists as a bcrypt hash on the account and in this message, and nowhere else.
     * A lost one is replaced through forgot-password, not looked up.
     */
    void sendAccountCredentialsEmail(String to, String name, String username, String temporaryPassword);

    /**
     * Generic subject/body email used by the notifications module
     * (POST /api/v1/notifications/send with type=EMAIL) — reuses the same
     * JavaMailSender plumbing as the password-reset/welcome templates instead
     * of standing up a second email mechanism.
     */
    void sendGenericNotification(String to, String subject, String message);
}
