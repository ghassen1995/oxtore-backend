package com.oxtore.backend.identity.otp;

import com.oxtore.backend.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One issued verification code. Only an HMAC of the code is stored (see OtpHasher).
 * A code is usable while consumedAt and invalidatedAt are both null and expiresAt is in the future.
 */
@Entity
@Table(name = "otp_codes", indexes = {
        @Index(name = "idx_otp_codes_user_purpose_created", columnList = "user_id, purpose, created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OtpPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OtpChannel channel;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Wrong guesses so far; the code is invalidated when it reaches otp.max-attempts. */
    @Column(nullable = false)
    private int attempts;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    /** Set when a newer code is sent or too many wrong attempts were made. */
    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "request_ip", length = 45)
    private String requestIp;

    public OtpCode(User user, OtpPurpose purpose, OtpChannel channel, String codeHash,
                   Instant createdAt, Instant expiresAt, String requestIp) {
        this.user = user;
        this.purpose = purpose;
        this.channel = channel;
        this.codeHash = codeHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.requestIp = requestIp;
    }
}
