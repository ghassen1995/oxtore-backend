package com.oxtore.backend.identity.service;

import com.oxtore.backend.identity.DTO.CompleteProfileRequest;
import com.oxtore.backend.identity.DTO.TokenPairResponse;
import com.oxtore.backend.identity.DTO.UpdateSettingsRequest;
import com.oxtore.backend.identity.DTO.UserSettingsResponse;
import com.oxtore.backend.identity.command.LoginCommand;
import com.oxtore.backend.identity.command.UserCommand;
import com.oxtore.backend.identity.entity.User;

public interface IdentityService {
    User createUser(UserCommand user);
    User createGoogleUser(String email, String firstName, String lastName, String googleSubject, String picture);
    User authenticateUser(LoginCommand command);
    User completeProfile(Long userId, CompleteProfileRequest request);
    User updateMe(Long userId, String fullName, String phone, String avatar);
    UserSettingsResponse getSettings(Long userId);
    UserSettingsResponse updateSettings(Long userId, UpdateSettingsRequest request);
    String generateAuthToken(User user);
    String generateRefreshToken(User user);
    TokenPairResponse refresh(String refreshToken);
    /** Replaces the password with an unknown random one and invalidates existing refresh tokens (not saved). */
    void revokePassword(User user);
}
