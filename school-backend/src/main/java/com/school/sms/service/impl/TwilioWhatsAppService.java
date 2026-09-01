package com.school.sms.service.impl;

import com.school.sms.service.WhatsAppService;
import com.school.sms.util.PhoneNumbers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Sends WhatsApp messages through Twilio's REST API.
 *
 * <p>Identical to {@link TwilioSmsService} except that both {@code From} and
 * {@code To} are prefixed with {@code whatsapp:}.  Twilio routes those to its
 * WhatsApp channel rather than the PSTN SMS path.
 *
 * <p>Prerequisites on the Twilio side:
 * <ul>
 *   <li>Join the WhatsApp Sandbox once per device (during development), or
 *   <li>Use an approved WhatsApp Business sender number (for production).
 * </ul>
 *
 * <p>The {@code fromNumber} injected here must already carry the {@code whatsapp:}
 * prefix (e.g. {@code whatsapp:+14155238886}); see {@link com.school.sms.config.SmsConfig}.
 */
@Slf4j
public class TwilioWhatsAppService implements WhatsAppService {

    private static final String API_ROOT = "https://api.twilio.com/2010-04-01/Accounts/";
    private static final String WHATSAPP_PREFIX = "whatsapp:";

    private final RestClient restClient;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;     // already prefixed: whatsapp:+14155238886
    private final String defaultCountryCode;

    public TwilioWhatsAppService(RestClient.Builder builder,
                                 String accountSid,
                                 String authToken,
                                 String fromNumber,
                                 String defaultCountryCode) {
        this.restClient = builder.build();
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.defaultCountryCode = defaultCountryCode;
    }

    @Override
    public boolean send(String toPhone, String message) {
        String e164 = PhoneNumbers.toE164(toPhone, defaultCountryCode);
        if (e164.isBlank()) {
            log.warn("Refusing WhatsApp send: no usable number");
            return false;
        }
        String to = WHATSAPP_PREFIX + e164;

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", to);
        form.add("From", fromNumber);
        form.add("Body", message);

        try {
            restClient.post()
                    .uri(API_ROOT + accountSid + "/Messages.json")
                    .header("Authorization", basicAuth())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();

            log.info("WhatsApp message sent to {}", mask(e164));
            return true;
        } catch (Exception ex) {
            // Swallowed: a gateway outage must not propagate as a 500 to the caller.
            log.error("Failed to send WhatsApp to {}: {}", mask(e164), ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private String basicAuth() {
        return "Basic " + Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
    }

    private String mask(String phone) {
        return phone.length() < 4 ? "****" : "****" + phone.substring(phone.length() - 4);
    }
}
