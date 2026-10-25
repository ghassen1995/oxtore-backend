package com.oxtore.backend.identity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Code is required")
        @Pattern(regexp = "\\d{6}", message = "The code must be 6 digits")
        String code,

        @NotBlank(message = "Password is required")
        // 72 bytes is BCrypt's input limit; same policy as sign-up
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String newPassword
) {
    /** Keeps the code and password out of DEBUG request logging. */
    @Override
    public String toString() {
        return "ResetPasswordRequest[email=" + email + ", code=***, newPassword=***]";
    }
}
