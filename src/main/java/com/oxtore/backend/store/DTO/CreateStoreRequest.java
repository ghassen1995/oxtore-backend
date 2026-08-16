package com.oxtore.backend.store.DTO;

import com.oxtore.backend.store.enums.BusinessSector;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStoreRequest(
        @NotBlank @Size(max = 100) String storeName,
        @Size(max = 150) String slogan,
        @NotNull BusinessSector businessSector,
        @NotBlank @Size(max = 150) String location,
        @NotBlank @Size(max = 20) String phoneNumber,
        @NotBlank @Email @Size(max = 100) String email
) {}