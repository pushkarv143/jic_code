package com.school.sms.config;

import com.school.sms.service.SmsService;
import com.school.sms.service.impl.LoggingSmsServiceImpl;
import com.school.sms.service.impl.TwilioSmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Chooses the SMS gateway.
 *
 * One bean, chosen here rather than by annotations on the two implementations.
 * The stub previously carried {@code @ConditionalOnMissingBean} naming a bean that
 * never existed, so it would have won even once a real gateway was added — an
 * explicit choice in one place cannot go wrong that quietly.
 *
 * Configure by setting the three Twilio properties — as environment variables in
 * the compose file, since they are credentials:
 *
 * <pre>
 *   APP_SMS_TWILIO_ACCOUNT_SID
 *   APP_SMS_TWILIO_AUTH_TOKEN
 *   APP_SMS_TWILIO_FROM_NUMBER
 * </pre>
 *
 * Leave the SID blank and the application still starts, with SMS reporting itself
 * unavailable and passcodes over that channel refused with a clear message rather
 * than accepted and dropped.
 */
@Slf4j
@Configuration
public class SmsConfig {

    @Value("${app.sms.twilio.account-sid:}")
    private String accountSid;

    @Value("${app.sms.twilio.auth-token:}")
    private String authToken;

    @Value("${app.sms.twilio.from-number:}")
    private String fromNumber;

    /** Applied to numbers stored without one. India, matching the data. */
    @Value("${app.sms.default-country-code:+91}")
    private String defaultCountryCode;

    @Bean
    public SmsService smsService(RestClient.Builder restClientBuilder) {
        // All three matter: a SID with no token cannot authenticate, and neither
        // can send without a number to send from. Half-configured is not configured.
        if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank()) {
            log.info("SMS gateway not configured; passcodes by SMS will be refused");
            return new LoggingSmsServiceImpl();
        }

        log.info("SMS gateway configured (Twilio), sending from {}", fromNumber);
        return new TwilioSmsService(restClientBuilder, accountSid, authToken, fromNumber, defaultCountryCode);
    }
}
