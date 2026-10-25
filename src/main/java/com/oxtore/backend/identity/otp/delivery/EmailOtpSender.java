package com.oxtore.backend.identity.otp.delivery;

import com.oxtore.backend.identity.otp.OtpChannel;
import com.oxtore.backend.identity.otp.OtpPurpose;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.time.Duration;

/** SMTP delivery through Spring Mail (spring.mail.* ← MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD). */
@Component
public class EmailOtpSender implements OtpSender {

    /** Subject, heading and intro line for each purpose. */
    private record Wording(String subject, String heading, String intro) {}

    private static Wording wording(OtpPurpose purpose) {
        return switch (purpose) {
            case SIGNUP_VERIFICATION -> new Wording("Your Oxtore verification code", "Verify your email",
                    "Enter this code to finish creating your Oxtore account:");
            case PASSWORD_RESET -> new Wording("Your Oxtore password reset code", "Reset your password",
                    "Enter this code in the app to choose a new password:");
        };
    }

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String mailHost;
    private final String fromEmail;
    private final String fromName;

    public EmailOtpSender(ObjectProvider<JavaMailSender> mailSender,
                          @Value("${spring.mail.host:}") String mailHost,
                          @Value("${app.mail.from-email:}") String fromEmail,
                          @Value("${app.mail.from-name:Oxtore}") String fromName) {
        this.mailSender = mailSender;
        this.mailHost = mailHost;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    @Override
    public OtpChannel channel() {
        return OtpChannel.EMAIL;
    }

    @Override
    public boolean isConfigured() {
        return !mailHost.isBlank() && !fromEmail.isBlank() && mailSender.getIfAvailable() != null;
    }

    @Override
    public void send(String destination, String code, Duration validity, OtpPurpose purpose) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) throw new OtpDeliveryException("Mail sender not configured");
        Wording wording = wording(purpose);
        long minutes = validity.toMinutes();
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(destination);
            helper.setSubject(wording.subject());
            helper.setText(plainText(wording, code, minutes), html(wording, code, minutes));
            sender.send(message);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            // Exception messages come from the SMTP exchange, not from the message body, so they never hold the code.
            throw new OtpDeliveryException("SMTP delivery failed: " + e.getClass().getSimpleName(), e);
        }
    }

    private static String plainText(Wording wording, String code, long minutes) {
        return wording.intro() + " " + code + "\n\n"
                + "It expires in " + minutes + " minutes. If you didn't request it, you can ignore this email.";
    }

    /** Same layout as the NestJS backend's email so users see a consistent message. */
    private static String html(Wording wording, String code, long minutes) {
        return """
                <div style="font-family: -apple-system, Segoe UI, Roboto, Arial, sans-serif; max-width: 480px; margin: 0 auto; background: #f8fafc;">
                  <div style="background: #1d4ed8; padding: 28px 24px; text-align: center; border-radius: 12px 12px 0 0;">
                    <span style="display: inline-block; font-size: 24px; font-weight: 900; letter-spacing: 3px; color: #ffffff;">
                      OX<span style="color: #6ee7b7;">TORE</span>
                    </span>
                  </div>
                  <div style="background: #ffffff; padding: 32px 24px; border-radius: 0 0 12px 12px; border: 1px solid #e5e7eb; border-top: none;">
                    <h2 style="color: #111827; font-size: 18px; margin: 0 0 8px;">%s</h2>
                    <p style="color: #4b5563; font-size: 14px; margin: 0 0 20px;">%s</p>
                    <div style="font-size: 32px; font-weight: 800; letter-spacing: 10px; color: #1d4ed8; padding: 18px; background: #eff6ff; border-radius: 10px; text-align: center;">
                      %s
                    </div>
                    <p style="color: #9ca3af; font-size: 12px; margin: 20px 0 0;">This code expires in %d minutes. If you didn't request it, you can safely ignore this email.</p>
                  </div>
                </div>
                """.formatted(wording.heading(), wording.intro(), code, minutes);
    }
}
