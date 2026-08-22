package com.school.sms.service.impl;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * What the credentials email actually contains.
 *
 * <p>Worth a test of its own because it is the only place a generated password is
 * ever readable. If the username and password do not both arrive intact, the
 * student cannot sign in and there is nowhere to look them up — the password is
 * stored only as a bcrypt hash. "The method was called" does not answer that; the
 * rendered message does, so this captures the real {@link MimeMessage} and reads it.
 *
 * <p>Two things it pins in particular: that a password containing HTML-significant
 * characters survives (the generated alphabet includes {@code &} and {@code <} is a
 * plausible future addition), and that the message says the password is temporary.
 * A credentials email that reads like a welcome invites the reader to file it and
 * keep using what it contains.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CredentialsEmailTest {

    @Mock private JavaMailSender mailSender;

    @InjectMocks private EmailServiceImpl service;

    private String bodyOf(String username, String password) throws Exception {
        // A real MimeMessage, so MimeMessageHelper behaves as it does in production
        // rather than against a mock that would accept anything.
        MimeMessage message = new jakarta.mail.internet.MimeMessage(
                jakarta.mail.Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@school.test");

        service.sendAccountCredentialsEmail("student@example.com", "Pushkar", username, password);

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sent.getValue().writeTo(out);
        return out.toString("UTF-8");
    }

    @Test
    @DisplayName("carries the username and the temporary password")
    void carriesBothCredentials() throws Exception {
        String body = bodyOf("pushkarv", "Kt7#mNqR4wYz");

        assertThat(body).contains("pushkarv");
        assertThat(body).contains("Kt7#mNqR4wYz");
    }

    @Test
    @DisplayName("says the password is temporary and will have to be changed")
    void saysItIsTemporary() throws Exception {
        String body = bodyOf("pushkarv", "Kt7#mNqR4wYz");

        // The reader has to understand this is not their password yet, or they file
        // the email and wonder later why it stopped working.
        assertThat(body).containsIgnoringCase("temporary");
        assertThat(body).containsIgnoringCase("first time you");
    }

    @Test
    @DisplayName("a password containing HTML-significant characters survives intact")
    void escapesWithoutCorrupting() throws Exception {
        // & is in the generated alphabet. Interpolated raw it would arrive as
        // &amp;-mangled or swallow the rest of the line, and the student would be
        // shown a password that is not the one on their account.
        String body = bodyOf("a&b", "Pw&7<x>Qm!zK");

        assertThat(body).contains("&amp;").contains("&lt;").contains("&gt;");
        // The literal must NOT appear unescaped — that is what would break rendering.
        assertThat(body).doesNotContain("Pw&7<x>Qm!zK");
    }

    @Test
    @DisplayName("addressed to the student, from the configured sender")
    void addressing() throws Exception {
        String body = bodyOf("pushkarv", "Kt7#mNqR4wYz");

        assertThat(body).contains("student@example.com");
        assertThat(body).contains("noreply@school.test");
        assertThat(body).contains("Greenwood School account");
    }

    @Test
    @DisplayName("a blank name still greets the reader rather than nobody")
    void blankName() throws Exception {
        MimeMessage message = new jakarta.mail.internet.MimeMessage(
                jakarta.mail.Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@school.test");

        service.sendAccountCredentialsEmail("student@example.com", "  ", "pushkarv", "Kt7#mNqR4wYz");

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sent.getValue().writeTo(out);

        assertThat(out.toString("UTF-8")).contains("Hello there");
    }
}
