package com.oxtore.backend.identity.otp;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * HMAC-SHA256 of the code with a server-side key (OTP_HMAC_SECRET). A plain or BCrypt hash of a 6-digit code
 * falls to an offline brute force of 10^6 candidates; with the key kept out of the DB, a DB leak alone reveals nothing.
 * The user id and purpose are mixed in so a hash can't be replayed for another account.
 */
@Component
public class OtpHasher {

    private static final String ALGORITHM = "HmacSHA256";
    private static final int MIN_SECRET_LENGTH = 32;

    private final SecretKeySpec key;

    public OtpHasher(OtpProperties properties) {
        String secret = properties.hmacSecret();
        if (secret == null || secret.isBlank() || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "OTP_HMAC_SECRET must be set to a random value of at least " + MIN_SECRET_LENGTH + " characters");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String hash(Long userId, OtpPurpose purpose, String code) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] digest = mac.doFinal((userId + ":" + purpose.name() + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    /** Constant-time comparison. */
    public boolean matches(String storedHash, Long userId, OtpPurpose purpose, String code) {
        byte[] expected = storedHash.getBytes(StandardCharsets.US_ASCII);
        byte[] actual = hash(userId, purpose, code).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, actual);
    }
}
