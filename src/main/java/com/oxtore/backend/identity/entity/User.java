package com.oxtore.backend.identity.entity;

import com.oxtore.backend.identity.enums.UserRole;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 8)
    private String publicId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, name = "password")
    private String passwordHash;

    @Column(name = "active_store_id")
    private Long activeStoreId;

    private String firstName;
    private String lastName;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    /**
     * The DB default (true) only backfills rows that existed before email verification was introduced, so old
     * accounts are treated as verified. New sign-ups always write false explicitly through the constructor.
     */
    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean default true")
    private boolean emailVerified;

    /** Storage key of the profile picture (see ImageStorageService), e.g. "avatars/{uuid}.jpg", or a Google photo URL. */
    @Column(name = "avatar")
    private String avatar;

    /** Last password reset; refresh tokens issued before it are rejected. */
    @Column(name = "credentials_changed_at")
    private Instant credentialsChangedAt;

    /** Google account id ("sub" claim) once the user has signed in with Google. */
    @Column(name = "google_subject", unique = true)
    private String googleSubject;

    /** Account creation ("member since"). The DB default only backfills rows that predate the column. */
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "timestamp(6) with time zone default now()")
    private Instant createdAt;

    // App settings (GET/PATCH /api/users/me/settings). Language and currency stay null until the user saves them,
    // so the app keeps the language/currency chosen on the device instead of being overwritten by a default.
    @Column(name = "settings_language", length = 2)
    private String settingsLanguage;

    @Column(name = "settings_currency", length = 3)
    private String settingsCurrency;

    @Column(name = "email_notifications", nullable = false, columnDefinition = "boolean default true")
    private boolean emailNotifications = true;

    @Column(name = "push_notifications", nullable = false, columnDefinition = "boolean default true")
    private boolean pushNotifications = true;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public User() {}

    public User(String publicId, String email, String passwordHash, String firstName,
                         String lastName, String phoneNumber, UserRole role) {
        this.publicId = publicId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.emailVerified = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public Long getActiveStoreId() { return activeStoreId; }
    public void setActiveStoreId(Long activeStoreId) { this.activeStoreId = activeStoreId; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Instant getCredentialsChangedAt() { return credentialsChangedAt; }
    public void setCredentialsChangedAt(Instant credentialsChangedAt) { this.credentialsChangedAt = credentialsChangedAt; }
    public String getGoogleSubject() { return googleSubject; }
    public void setGoogleSubject(String googleSubject) { this.googleSubject = googleSubject; }
    public Instant getCreatedAt() { return createdAt; }
    public String getSettingsLanguage() { return settingsLanguage; }
    public void setSettingsLanguage(String settingsLanguage) { this.settingsLanguage = settingsLanguage; }
    public String getSettingsCurrency() { return settingsCurrency; }
    public void setSettingsCurrency(String settingsCurrency) { this.settingsCurrency = settingsCurrency; }
    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }
    public boolean isPushNotifications() { return pushNotifications; }
    public void setPushNotifications(boolean pushNotifications) { this.pushNotifications = pushNotifications; }
}