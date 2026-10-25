package com.oxtore.backend.identity.otp;

/**
 * What a code proves. Adding a value requires the otp_codes purpose check constraint to be gone
 * (see OtpSchemaMaintenance): Hibernate created it with the original values and ddl-auto=update never alters it.
 */
public enum OtpPurpose {
    SIGNUP_VERIFICATION,
    PASSWORD_RESET
}
