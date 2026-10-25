package com.oxtore.backend.identity.otp.delivery;

import com.oxtore.backend.identity.otp.OtpChannel;
import com.oxtore.backend.identity.otp.OtpPurpose;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * WhatsApp delivery through Twilio's Messages API (same provider as the NestJS backend), called over REST.
 * Enabled only with WHATSAPP_PROVIDER=twilio and TWILIO_ACCOUNT_SID / TWILIO_AUTH_TOKEN / TWILIO_PHONE set.
 * <p>
 * Production note: outside Twilio's sandbox, a business-initiated WhatsApp message must use an approved template.
 * Set TWILIO_WHATSAPP_CONTENT_SID to that template (with the code as variable {{1}}); without it a free-form body is
 * sent, which Twilio only delivers in the sandbox or inside a 24-hour session.
 */
@Component
public class TwilioWhatsAppOtpSender implements OtpSender {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String provider;
    private final String accountSid;
    private final String authToken;
    private final String fromPhone;
    private final String contentSid;
    private final RestClient restClient;

    public TwilioWhatsAppOtpSender(@Value("${whatsapp.provider:none}") String provider,
                                   @Value("${twilio.account-sid:}") String accountSid,
                                   @Value("${twilio.auth-token:}") String authToken,
                                   @Value("${twilio.phone:}") String fromPhone,
                                   @Value("${twilio.whatsapp-content-sid:}") String contentSid,
                                   @Value("${twilio.api-base-url:https://api.twilio.com}") String apiBaseUrl) {
        this.provider = provider;
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromPhone = fromPhone;
        this.contentSid = contentSid;
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
        requestFactory.setReadTimeout(TIMEOUT);
        this.restClient = RestClient.builder().baseUrl(apiBaseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public OtpChannel channel() {
        return OtpChannel.WHATSAPP;
    }

    @Override
    public boolean isConfigured() {
        return "twilio".equalsIgnoreCase(provider)
                && !accountSid.isBlank() && !authToken.isBlank() && !fromPhone.isBlank();
    }

    @Override
    public void send(String destination, String code, Duration validity, OtpPurpose purpose) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("From", "whatsapp:" + fromPhone);
        form.add("To", "whatsapp:" + destination);
        if (!contentSid.isBlank()) {
            form.add("ContentSid", contentSid);
            form.add("ContentVariables", "{\"1\":\"" + code + "\"}"); // code is digits only
        } else {
            form.add("Body", (purpose == OtpPurpose.PASSWORD_RESET
                    ? "Your Oxtore password reset code is: "
                    : "Your Oxtore verification code is: ") + code);
        }

        try {
            restClient.post()
                    .uri("/2010-04-01/Accounts/{sid}/Messages.json", accountSid)
                    .headers(h -> h.setBasicAuth(accountSid, authToken))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            // Only the status is kept: never echo request details, which contain the code.
            throw new OtpDeliveryException("Twilio rejected the message with HTTP " + e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new OtpDeliveryException("Twilio unreachable: " + e.getClass().getSimpleName());
        }
    }
}
