-- Refresh-token invalidation on password reset, Google sign-in, password-reset codes.
-- With spring.jpa.hibernate.ddl-auto=update Hibernate adds the columns; the constraint drop is also run at startup by
-- OtpSchemaMaintenance. Run by hand only where ddl-auto is off. Idempotent.

BEGIN;

-- Refresh tokens issued before this instant are rejected (set by reset-password).
ALTER TABLE users ADD COLUMN IF NOT EXISTS credentials_changed_at timestamp(6) with time zone;

-- Google account id ("sub"), set on first Google sign-in.
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_subject varchar(255);
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_google_subject ON users (google_subject);

-- Hibernate created this check with SIGNUP_VERIFICATION only; PASSWORD_RESET codes need it gone.
ALTER TABLE IF EXISTS otp_codes DROP CONSTRAINT IF EXISTS otp_codes_purpose_check;

COMMIT;
