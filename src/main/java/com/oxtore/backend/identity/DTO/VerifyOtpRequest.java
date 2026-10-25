package com.oxtore.backend.identity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Code is required")
        @Pattern(regexp = "\\d{6}", message = "The code must be 6 digits")
        String code
) {
    /** Keeps the code out of DEBUG request logging. */
    @Override
    public String toString() {
        return "VerifyOtpRequest[email=" + email + ", code=***]";
    }
}
