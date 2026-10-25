package com.oxtore.backend.identity.otp;

import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.exceptions.OtpException;
import com.oxtore.backend.identity.otp.delivery.OtpDeliveryException;
import com.oxtore.backend.identity.otp.delivery.OtpSenderRegistry;
import com.oxtore.backend.identity.repository.IdentityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Database side of OTP: issuing (with delivery inside the transaction) and checking codes, per purpose.
 * Sign-up codes only exist for unverified accounts; password-reset codes for any account.
 */
@Slf4j
@Service
public class OtpCodeService {

    private final OtpCodeRepository otpCodeRepository;
    private final IdentityRepository userRepository;
    private final OtpHasher hasher;
    private final OtpSenderRegistry senders;
    private final OtpProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public OtpCodeService(OtpCodeRepository otpCodeRepository, IdentityRepository userRepository, OtpHasher hasher,
                          OtpSenderRegistry senders, OtpProperties properties, Clock clock) {
        this.otpCodeRepository = otpCodeRepository;
        this.userRepository = userRepository;
        this.hasher = hasher;
        this.senders = senders;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Issues and delivers a new code. Emails that can't receive one (unknown, or already verified for sign-up) return
     * silently so the caller answers the same way. Delivery happens inside the transaction: if it fails, the new code
     * is rolled back and the previous one stays valid, so nothing counts as sent.
     */
    @Transactional
    public void issue(String email, OtpChannel channel, String requestIp, OtpPurpose purpose) {
        Optional<User> found = userRepository.findByEmailIgnoreCase(email);
        if (found.isEmpty() || !eligible(found.get(), purpose)) return;

        User user = userRepository.findByIdForUpdate(found.get().getId()).orElseThrow();
        if (!eligible(user, purpose)) return;

        Instant now = clock.instant();
        Optional<OtpCode> last = otpCodeRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose);
        if (last.isPresent()) {
            long wait = secondsUntil(last.get().getCreatedAt().plus(properties.resendCooldown()), now);
            if (wait > 0) throw OtpException.cooldown(wait);
        }
        long sentLastHour = otpCodeRepository.countByUserIdAndPurposeAndCreatedAtAfter(
                user.getId(), purpose, now.minus(Duration.ofHours(1)));
        if (sentLastHour >= properties.maxSendsPerEmailPerHour()) throw OtpException.rateLimited(null);

        String destination = channel == OtpChannel.WHATSAPP ? toE164(user.getPhoneNumber()) : user.getEmail();
        String code = "%06d".formatted(random.nextInt(1_000_000));

        otpCodeRepository.invalidateActive(user.getId(), purpose, now);
        otpCodeRepository.save(new OtpCode(user, purpose, channel, hasher.hash(user.getId(), purpose, code),
                now, now.plus(properties.ttl()), requestIp));

        try {
            senders.deliver(channel, destination, code, properties.ttl(), purpose);
        } catch (OtpDeliveryException e) {
            log.warn("OTP delivery via {} failed for user {}: {}", channel, user.getId(), e.getMessage());
            throw OtpException.deliveryFailed();
        }
    }

    /** Sign-up verification: consumes the code and marks the email verified. */
    @Transactional(noRollbackFor = OtpException.class)
    public void verifySignup(String email, String code) {
        consume(email, code, OtpPurpose.SIGNUP_VERIFICATION).setEmailVerified(true);
    }

    /**
     * Checks a code and consumes it on success, returning its (managed) user. Wrong guesses are counted even though an
     * exception is thrown (noRollbackFor), and the code is invalidated at otp.max-attempts. Callers that add their own
     * changes must run in a transaction with the same noRollbackFor rule.
     */
    @Transactional(noRollbackFor = OtpException.class)
    public User consume(String email, String code, OtpPurpose purpose) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow(OtpException::invalid);
        if (!eligible(user, purpose)) throw OtpException.invalid();

        OtpCode otp = otpCodeRepository
                .findFirstByUserIdAndPurposeAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(user.getId(), purpose)
                .orElseThrow(OtpException::invalid);

        Instant now = clock.instant();
        if (!now.isBefore(otp.getExpiresAt())) throw OtpException.expired();

        if (!hasher.matches(otp.getCodeHash(), user.getId(), purpose, code)) {
            otp.setAttempts(otp.getAttempts() + 1);
            if (otp.getAttempts() >= properties.maxAttempts()) {
                otp.setInvalidatedAt(now);
                throw OtpException.tooManyAttempts();
            }
            throw OtpException.invalid();
        }

        otp.setConsumedAt(now);
        return user;
    }

    private static boolean eligible(User user, OtpPurpose purpose) {
        return purpose != OtpPurpose.SIGNUP_VERIFICATION || !user.isEmailVerified();
    }

    /** Accepts common formatting ("+216 12 345 678", "+216-12345678"); WhatsApp needs strict E.164. */
    static String toE164(String phone) {
        if (phone == null) throw OtpException.invalidPhone();
        String compact = phone.replaceAll("[\\s\\-().]", "");
        if (compact.startsWith("00")) compact = "+" + compact.substring(2);
        if (!compact.matches("\\+[1-9]\\d{6,14}")) throw OtpException.invalidPhone();
        return compact;
    }

    private static long secondsUntil(Instant target, Instant now) {
        long millis = Duration.between(now, target).toMillis();
        return millis <= 0 ? 0 : (millis + 999) / 1000;
    }
}
