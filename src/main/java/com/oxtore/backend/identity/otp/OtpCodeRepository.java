package com.oxtore.backend.identity.otp;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(Long userId, OtpPurpose purpose);

    long countByUserIdAndPurposeAndCreatedAtAfter(Long userId, OtpPurpose purpose, Instant since);

    /** The code currently usable for verification (if any), locked so concurrent guesses are counted one by one. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpCode> findFirstByUserIdAndPurposeAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
            Long userId, OtpPurpose purpose);

    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("""
            update OtpCode c set c.invalidatedAt = :now
            where c.user.id = :userId and c.purpose = :purpose
              and c.consumedAt is null and c.invalidatedAt is null
            """)
    int invalidateActive(@Param("userId") Long userId, @Param("purpose") OtpPurpose purpose, @Param("now") Instant now);
}
