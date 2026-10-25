-- Sign-up email verification + OTP codes.
-- With spring.jpa.hibernate.ddl-auto=update (the current setting) Hibernate applies the equivalent automatically.
-- Run this by hand only in an environment where ddl-auto is off. It is idempotent.

BEGIN;

-- Existing accounts predate verification: the DEFAULT true backfills them as verified.
-- The application always writes the value explicitly (new sign-ups = false).
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified boolean NOT NULL DEFAULT true;

-- Profile picture storage key (e.g. avatars/{uuid}.jpg)
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar varchar(255);

CREATE TABLE IF NOT EXISTS otp_codes (
    id             bigserial PRIMARY KEY,
    user_id        bigint       NOT NULL REFERENCES users (id),
    purpose        varchar(32)  NOT NULL,
    channel        varchar(16)  NOT NULL,
    code_hash      varchar(64)  NOT NULL,   -- HMAC-SHA256 hex, never the code itself
    expires_at     timestamp(6) with time zone NOT NULL,
    attempts       integer      NOT NULL,
    consumed_at    timestamp(6) with time zone,
    invalidated_at timestamp(6) with time zone,
    created_at     timestamp(6) with time zone NOT NULL,
    request_ip     varchar(45)
);

CREATE INDEX IF NOT EXISTS idx_otp_codes_user_purpose_created ON otp_codes (user_id, purpose, created_at);

COMMIT;

-- Checks
-- All pre-existing users verified:
--   SELECT count(*) FILTER (WHERE email_verified) AS verified, count(*) AS total FROM users;
-- Emails differing only by case (logins are now case-insensitive, these would conflict):
--   SELECT lower(email), count(*) FROM users GROUP BY 1 HAVING count(*) > 1;
