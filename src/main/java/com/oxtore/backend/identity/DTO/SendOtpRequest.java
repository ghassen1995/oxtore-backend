package com.oxtore.backend.identity.DTO;

import com.oxtore.backend.identity.otp.OtpChannel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendOtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email,

        @NotNull(message = "method must be \"email\" or \"whatsapp\"")
        OtpChannel method
) {}
