package com.school.sms.service.impl;

import com.school.sms.service.SmsService;
import com.school.sms.util.PhoneNumbers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Sends SMS through Twilio's REST API.
 *
 * Called over plain HTTP with the RestClient already on the classpath rather than
 * through Twilio's SDK. The SDK pulls in a sizeable dependency tree for what is
 * one form-encoded POST, and the server this runs on has a few hundred megabytes
 * of memory to spare — see the deployment notes. The whole integration is the
 * thirty lines below.
 *
 * Twilio wants E.164 (+919810011122). The stored numbers are not in that shape and
 * neither is what users type, so everything is normalised on the way out.
 *
 * Swapping provider means writing one more class like this one: nothing in
 * OtpService knows which gateway is behind {@link SmsService}. For Indian traffic
 * at volume MSG91 or another domestic route is usually the better commercial
 * choice, and reaching +91 numbers through any provider needs DLT registration
 * before it will deliver reliably.
 */
@Slf4j
public class TwilioSmsService implements SmsService {

    private static final String API_ROOT = "https://api.twilio.com/2010-04-01/Accounts/";

    private final RestClient restClient;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final String defaultCountryCode;

    public TwilioSmsService(RestClient.Builder builder,
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
        String to = PhoneNumbers.toE164(toPhone, defaultCountryCode);
        if (to.isBlank()) {
            log.warn("Refusing to send SMS: no usable number");
            return false;
        }

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

            // The number is masked and the body is never logged — for an OTP the
            // body is the code, and logs are the easiest place in the system to read.
            log.info("SMS sent to {}", mask(to));
            return true;
        } catch (Exception ex) {
            // Swallowed rather than rethrown: a gateway outage should not turn into
            // a 500 that tells the caller their number is or is not registered.
            // The caller already got the same neutral answer either way.
            log.error("Failed to send SMS to {}: {}", mask(to), ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private String basicAuth() {
        String credentials = accountSid + ":" + authToken;
        return "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /** Enough to match against a support call, not enough to be a contact list. */
    private String mask(String phone) {
        return phone.length() < 4 ? "****" : "****" + phone.substring(phone.length() - 4);
    }
}
