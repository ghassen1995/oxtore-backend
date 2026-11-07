package com.oxtore.backend.identity.DTO;

import com.oxtore.backend.common.validation.InputRules;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/users/me. Every field is optional: null leaves the stored value unchanged.
 * avatar must be a URL returned by POST /api/uploads/images?folder=avatars. The email can't be changed here.
 * Names accept Unicode letters (Arabic, accents...) but no digits; phones 8 to 15 digits and no letters.
 */
public record UpdateMeRequest(
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        @Pattern(regexp = InputRules.PERSON_NAME_REGEX, message = InputRules.PERSON_NAME_MESSAGE)
        String fullName,

        @Size(max = 30, message = "Phone number is too long")
        @Pattern(regexp = InputRules.PHONE_REGEX, message = InputRules.PHONE_MESSAGE)
        String phone,

        String avatar
) {}
