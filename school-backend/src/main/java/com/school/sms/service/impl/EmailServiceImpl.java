package com.school.sms.service.impl;

import com.school.sms.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Override
    @Async("taskExecutor")
    public void sendPasswordResetEmail(String to, String recipientName, String resetLink) {
        String subject = "Reset your School Management System password";
        String body = buildPasswordResetHtml(recipientName, resetLink);
        send(to, subject, body);
    }

    @Override
    @Async("taskExecutor")
    public void sendWelcomeEmail(String to, String name, String username) {
        String subject = "Welcome to the School Management System";
        String body = buildWelcomeHtml(name, username);
        send(to, subject, body);
    }

    @Override
    @Async("taskExecutor")
    public void sendGenericNotification(String to, String subject, String message) {
        String body = buildGenericHtml(subject, message);
        send(to, subject, body);
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} with subject '{}'", to, subject);
        } catch (MessagingException ex) {
            log.error("Failed to build email to {}: {}", to, ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
        }
    }

    private String buildPasswordResetHtml(String recipientName, String resetLink) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background-color:#f4f5f7;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f5f7;padding:24px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;">
                          <tr>
                            <td style="background-color:#1f2a44;padding:20px 32px;">
                              <h2 style="color:#ffffff;margin:0;font-size:18px;">School Management System</h2>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="font-size:15px;color:#333333;">Hello %s,</p>
                              <p style="font-size:15px;color:#333333;">
                                We received a request to reset your password. Click the button below to
                                choose a new one. This link expires in 30 minutes.
                              </p>
                              <p style="text-align:center;margin:32px 0;">
                                <a href="%s" style="background-color:#2563eb;color:#ffffff;text-decoration:none;
                                   padding:12px 28px;border-radius:6px;font-size:15px;display:inline-block;">
                                  Reset Password
                                </a>
                              </p>
                              <p style="font-size:13px;color:#666666;">
                                If you did not request a password reset, you can safely ignore this email.
                              </p>
                              <p style="font-size:13px;color:#666666;">
                                If the button does not work, copy and paste this link into your browser:<br/>
                                <a href="%s" style="color:#2563eb;word-break:break-all;">%s</a>
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:16px 32px;background-color:#f4f5f7;">
                              <p style="font-size:12px;color:#999999;margin:0;">
                                This is an automated message, please do not reply.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(recipientName, resetLink, resetLink, resetLink);
    }

    private String buildWelcomeHtml(String name, String username) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background-color:#f4f5f7;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f5f7;padding:24px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;">
                          <tr>
                            <td style="background-color:#1f2a44;padding:20px 32px;">
                              <h2 style="color:#ffffff;margin:0;font-size:18px;">School Management System</h2>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="font-size:15px;color:#333333;">Hello %s,</p>
                              <p style="font-size:15px;color:#333333;">
                                Your account has been created successfully. Here are your login details:
                              </p>
                              <p style="font-size:15px;color:#333333;">
                                <strong>Username:</strong> %s
                              </p>
                              <p style="font-size:13px;color:#666666;">
                                For security reasons, please log in and change your password if this is a
                                temporary or shared credential.
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:16px 32px;background-color:#f4f5f7;">
                              <p style="font-size:12px;color:#999999;margin:0;">
                                This is an automated message, please do not reply.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(name, username);
    }

    private String buildGenericHtml(String subject, String message) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background-color:#f4f5f7;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f5f7;padding:24px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;">
                          <tr>
                            <td style="background-color:#1f2a44;padding:20px 32px;">
                              <h2 style="color:#ffffff;margin:0;font-size:18px;">%s</h2>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="font-size:15px;color:#333333;white-space:pre-line;">%s</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:16px 32px;background-color:#f4f5f7;">
                              <p style="font-size:12px;color:#999999;margin:0;">
                                This is an automated message, please do not reply.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(subject, message);
    }
}
