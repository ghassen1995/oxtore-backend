package com.oxtore.backend.identity.service;

import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.exceptions.AuthException;
import com.oxtore.backend.identity.google.GoogleIdentity;
import com.oxtore.backend.identity.google.GoogleTokenVerifier;
import com.oxtore.backend.identity.otp.OtpService;
import com.oxtore.backend.identity.repository.IdentityRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Sign-in with a Google ID token. Lookup order: the linked Google account, then an existing account with the same
 * (Google-verified) email, which gets linked, then a new account. Not @Transactional: account creation goes through
 * IdentityService's per-attempt transactions.
 */
@Service
public class GoogleAuthService {

    /** users.avatar is varchar(255); longer photo URLs are skipped rather than truncated. */
    private static final int MAX_AVATAR_LENGTH = 255;

    private final GoogleTokenVerifier verifier;
    private final IdentityRepository userRepository;
    private final IdentityService identityService;

    public GoogleAuthService(GoogleTokenVerifier verifier, IdentityRepository userRepository,
                             IdentityService identityService) {
        this.verifier = verifier;
        this.userRepository = userRepository;
        this.identityService = identityService;
    }

    public User login(String idToken) {
        if (!verifier.isConfigured()) throw AuthException.googleUnavailable();
        GoogleIdentity google = verifier.verify(idToken);
        // Linking by email is only safe when Google vouches for the address.
        if (!google.emailVerified() || google.email() == null) throw AuthException.googleEmailNotVerified();

        Optional<User> linked = userRepository.findByGoogleSubject(google.subject());
        if (linked.isPresent()) return linked.get();

        String email = OtpService.normalize(google.email());
        String picture = google.picture() != null && google.picture().length() <= MAX_AVATAR_LENGTH
                ? google.picture() : null;

        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (!user.isEmailVerified()) {
                // Nobody proved ownership of this email before: whoever registered it (possibly not the owner) loses
                // the password and its refresh tokens; the real owner can set a new password via forgot-password.
                identityService.revokePassword(user);
            }
            user.setGoogleSubject(google.subject());
            user.setEmailVerified(true);
            if (user.getAvatar() == null) user.setAvatar(picture);
            return userRepository.save(user);
        }

        String[] names = names(google, email);
        return identityService.createGoogleUser(email, names[0], names[1], google.subject(), picture);
    }

    /** given/family name when Google provides them, else the display name split, else the email's local part. */
    private static String[] names(GoogleIdentity google, String email) {
        if (google.givenName() != null && !google.givenName().isBlank()) {
            return new String[]{google.givenName().trim(), google.familyName() == null ? "" : google.familyName().trim()};
        }
        if (google.name() != null && !google.name().isBlank()) {
            return IdentityServiceImpl.splitFullName(google.name());
        }
        return new String[]{email.substring(0, email.indexOf('@')), ""};
    }
}
