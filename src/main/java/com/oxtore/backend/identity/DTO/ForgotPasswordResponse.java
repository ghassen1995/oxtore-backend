package com.oxtore.backend.identity.DTO;

/** Same answer whether or not the email has an account. Times in seconds, as in SendOtpResponse. */
public record ForgotPasswordResponse(String message, int expiresIn, int resendIn) {}
