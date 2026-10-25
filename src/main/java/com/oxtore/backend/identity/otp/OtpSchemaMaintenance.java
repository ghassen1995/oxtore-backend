package com.oxtore.backend.identity.otp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate generated "otp_codes_purpose_check" listing only SIGNUP_VERIFICATION when it created the table, and
 * ddl-auto=update never alters existing check constraints, so PASSWORD_RESET rows would be rejected. Dropping it is
 * idempotent; the enum mapping still restricts the values the application writes. Same statement as
 * db/manual/2026-10-09_auth_password_reset_google.sql.
 */
@Slf4j
@Component
public class OtpSchemaMaintenance implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public OtpSchemaMaintenance(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE IF EXISTS otp_codes DROP CONSTRAINT IF EXISTS otp_codes_purpose_check");
        log.debug("otp_codes purpose check constraint removed (if present)");
    }
}
