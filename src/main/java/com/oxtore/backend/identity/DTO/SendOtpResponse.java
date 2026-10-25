package com.oxtore.backend.identity.DTO;

/**
 * Both values in seconds: how long the code stays valid, and how long before another send is accepted.
 * int on purpose: JacksonConfig serializes every long as a JSON string, and the app expects numbers here.
 */
public record SendOtpResponse(int expiresIn, int resendIn) {}
