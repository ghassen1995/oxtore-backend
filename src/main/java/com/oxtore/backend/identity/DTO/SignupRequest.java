package com.oxtore.backend.identity.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mirrors the Angular sign-up form: full name (min 2), valid email, phone required, password min 8. */
public record SignupRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email,

        @NotBlank(message = "Phone number is required")
        @Size(max = 30, message = "Phone number is too long")
        String phone,

        @NotBlank(message = "Password is required")
        // 72 bytes is BCrypt's input limit
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password
) {
    /** Keeps the password out of DEBUG request logging. */
    @Override
    public String toString() {
        return "SignupRequest[fullName=" + fullName + ", email=" + email + ", phone=" + phone + ", password=***]";
    }
}
