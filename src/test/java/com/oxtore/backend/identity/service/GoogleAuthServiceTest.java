package com.oxtore.backend.identity.service;

import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.enums.UserRole;
import com.oxtore.backend.identity.exceptions.AuthException;
import com.oxtore.backend.identity.google.GoogleIdentity;
import com.oxtore.backend.identity.google.GoogleTokenVerifier;
import com.oxtore.backend.identity.repository.IdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GoogleAuthServiceTest {

    private GoogleTokenVerifier verifier;
    private IdentityRepository users;
    private IdentityService identity;
    private GoogleAuthService service;

    private static GoogleIdentity google(boolean emailVerified) {
        return new GoogleIdentity("google-sub-1", "Jane.Doe@Gmail.com", emailVerified, "Jane Doe", "Jane", "Doe",
                "https://lh3.googleusercontent.com/a/photo");
    }

    @BeforeEach
    void setUp() {
        verifier = mock(GoogleTokenVerifier.class);
        users = mock(IdentityRepository.class);
        identity = mock(IdentityService.class);
        service = new GoogleAuthService(verifier, users, identity);
        when(verifier.isConfigured()).thenReturn(true);
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void notConfiguredIsUnavailable() {
        when(verifier.isConfigured()).thenReturn(false);
        assertThatThrownBy(() -> service.login("token"))
                .isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.getCode()).isEqualTo("GOOGLE_UNAVAILABLE"));
    }

    @Test
    void unverifiedGoogleEmailIsRefused() {
        when(verifier.verify("token")).thenReturn(google(false));
        assertThatThrownBy(() -> service.login("token"))
                .isInstanceOfSatisfying(AuthException.class,
                        e -> assertThat(e.getCode()).isEqualTo("GOOGLE_EMAIL_NOT_VERIFIED"));
        verifyNoInteractions(users, identity);
    }

    @Test
    void alreadyLinkedAccountSignsIn() {
        User linked = user(true);
        when(verifier.verify("token")).thenReturn(google(true));
        when(users.findByGoogleSubject("google-sub-1")).thenReturn(Optional.of(linked));

        assertThat(service.login("token")).isSameAs(linked);
        verify(identity, never()).createGoogleUser(any(), any(), any(), any(), any());
    }

    @Test
    void verifiedEmailAccountIsLinkedAndKeepsItsPassword() {
        User existing = user(true);
        when(verifier.verify("token")).thenReturn(google(true));
        when(users.findByGoogleSubject("google-sub-1")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("jane.doe@gmail.com")).thenReturn(Optional.of(existing));

        User result = service.login("token");

        assertThat(result.getGoogleSubject()).isEqualTo("google-sub-1");
        assertThat(result.getAvatar()).isEqualTo("https://lh3.googleusercontent.com/a/photo");
        verify(identity, never()).revokePassword(any());
    }

    @Test
    void unverifiedEmailAccountLosesItsPasswordWhenLinked() {
        User squatted = user(false);
        when(verifier.verify("token")).thenReturn(google(true));
        when(users.findByGoogleSubject("google-sub-1")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("jane.doe@gmail.com")).thenReturn(Optional.of(squatted));

        User result = service.login("token");

        verify(identity).revokePassword(squatted);
        assertThat(result.isEmailVerified()).isTrue();
    }

    @Test
    void newEmailCreatesVerifiedAccountWithGoogleNames() {
        when(verifier.verify("token")).thenReturn(google(true));
        when(users.findByGoogleSubject("google-sub-1")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("jane.doe@gmail.com")).thenReturn(Optional.empty());

        service.login("token");

        verify(identity).createGoogleUser("jane.doe@gmail.com", "Jane", "Doe", "google-sub-1",
                "https://lh3.googleusercontent.com/a/photo");
    }

    private static User user(boolean verified) {
        User u = new User("ABCD2345", "jane.doe@gmail.com", "hash", "Jane", "Doe", "+21612345678", UserRole.USER);
        u.setId(5L);
        u.setEmailVerified(verified);
        return u;
    }
}
