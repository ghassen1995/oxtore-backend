package com.oxtore.backend.identity.service;

import com.oxtore.backend.identity.DTO.CompleteProfileRequest;
import com.oxtore.backend.identity.DTO.TokenPairResponse;
import com.oxtore.backend.identity.DTO.UpdateSettingsRequest;
import com.oxtore.backend.identity.DTO.UserSettingsResponse;
import com.oxtore.backend.identity.command.LoginCommand;
import com.oxtore.backend.identity.command.UserCommand;
import com.oxtore.backend.identity.config.JwtUtil;
import com.oxtore.backend.identity.config.PublicIdGenerator;
import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.enums.UserRole;
import com.oxtore.backend.identity.exceptions.AuthException;
import com.oxtore.backend.identity.exceptions.AuthenticationFailedException;
import com.oxtore.backend.identity.exceptions.UserAlreadyExistsException;
import com.oxtore.backend.identity.repository.IdentityRepository;
import com.oxtore.backend.upload.ImageReferenceInput;
import com.oxtore.backend.upload.ImageStorageService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Function;

@Service
public class IdentityServiceImpl implements IdentityService {

    private static final int MAX_PUBLIC_ID_ATTEMPTS = 5;
    private static final String AVATAR_FOLDER = "avatars";

    private final IdentityRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PublicIdGenerator publicIdGenerator;
    private final ImageReferenceInput imageReferenceInput;
    private final ImageStorageService imageStorage;
    private final SecureRandom random = new SecureRandom();

    public IdentityServiceImpl(IdentityRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               JwtUtil jwtUtil,
                               PublicIdGenerator publicIdGenerator,
                               ImageReferenceInput imageReferenceInput,
                               ImageStorageService imageStorage) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.publicIdGenerator = publicIdGenerator;
        this.imageReferenceInput = imageReferenceInput;
        this.imageStorage = imageStorage;
    }

    @Override
    public User createUser(UserCommand command) {
        String email = command.email();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new UserAlreadyExistsException(email);
        }

        String passwordHash = passwordEncoder.encode(command.password());
        // role is always USER at registration — store-scoped roles live in staff-service
        return insertWithUniquePublicId(email, publicId -> new User(publicId, email, passwordHash, command.firstName(),
                command.lastName(), command.phoneNumber(), UserRole.USER));
    }

    /**
     * Account created from a verified Google identity: email already verified, no phone yet (the app asks for it),
     * and a random password nobody knows (the user can set one through forgot-password).
     */
    @Override
    public User createGoogleUser(String email, String firstName, String lastName, String googleSubject, String picture) {
        String unusablePasswordHash = unusablePasswordHash();

        return insertWithUniquePublicId(email, publicId -> {
            User user = new User(publicId, email, unusablePasswordHash, firstName, lastName, null, UserRole.USER);
            user.setEmailVerified(true);
            user.setGoogleSubject(googleSubject);
            user.setAvatar(picture);
            return user;
        });
    }

    @Override
    public User authenticateUser(LoginCommand command) {
        User user = userRepository.findByEmailIgnoreCase(command.email()).orElseThrow(
                () -> new AuthenticationFailedException("Invalid email or password")
        );

        boolean matches = passwordEncoder.matches(
                command.passwordHash(),
                user.getPasswordHash()
        );

        if (!matches) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        return user;
    }

    @Override
    @Transactional
    public User completeProfile(Long userId, CompleteProfileRequest request) {
        return updateMe(userId, request.fullName(), request.phone(), request.avatar());
    }

    /** Partial profile update: null (or blank) fields keep their stored value. The email is never changed here. */
    @Override
    @Transactional
    public User updateMe(Long userId, String fullName, String phone, String avatar) {
        User user = findUser(userId);

        if (fullName != null && !fullName.isBlank()) {
            String[] names = splitFullName(fullName);
            user.setFirstName(names[0]);
            user.setLastName(names[1]);
        }

        if (phone != null && !phone.isBlank()) {
            user.setPhoneNumber(phone.trim());
        }

        if (avatar != null && !avatar.isBlank() && !avatar.equals(imageUrlForStoredAvatar(user))) {
            String newKey = imageReferenceInput.optionalKey(avatar, AVATAR_FOLDER, "avatar");
            String oldKey = user.getAvatar();
            user.setAvatar(newKey);
            if (oldKey != null && !Objects.equals(oldKey, newKey)) {
                imageStorage.delete(oldKey); // no-op for non-key values such as a Google photo URL
            }
        }
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public UserSettingsResponse getSettings(Long userId) {
        return toSettings(findUser(userId));
    }

    @Override
    @Transactional
    public UserSettingsResponse updateSettings(Long userId, UpdateSettingsRequest request) {
        User user = findUser(userId);
        if (request.language() != null) user.setSettingsLanguage(request.language());
        if (request.currency() != null) user.setSettingsCurrency(request.currency());
        if (request.emailNotifications() != null) user.setEmailNotifications(request.emailNotifications());
        if (request.pushNotifications() != null) user.setPushNotifications(request.pushNotifications());
        return toSettings(user);
    }

    private static UserSettingsResponse toSettings(User user) {
        return new UserSettingsResponse(user.getSettingsLanguage(), user.getSettingsCurrency(),
                user.isEmailNotifications(), user.isPushNotifications());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    /** The avatar as the app received it (public URL for stored files), so re-sending it unchanged is a no-op. */
    private String imageUrlForStoredAvatar(User user) {
        String stored = user.getAvatar();
        if (stored == null) return null;
        return imageStorage.keyFromUrl(stored).filter(stored::equals).map(imageStorage::publicUrl).orElse(stored);
    }

    @Override
    public String generateAuthToken(User user) {
        return jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
    }

    @Override
    public String generateRefreshToken(User user) {
        return jwtUtil.generateRefreshToken(user.getId());
    }

    /**
     * Stateless refresh: a valid, unexpired refresh token for an existing user yields a new access + refresh pair.
     * Tokens issued before the user's last password reset are rejected.
     */
    @Override
    public TokenPairResponse refresh(String refreshToken) {
        Claims claims;
        Long userId;
        try {
            claims = jwtUtil.parseRefreshToken(refreshToken);
            userId = Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw AuthException.invalidRefreshToken();
        }

        User user = userRepository.findById(userId).orElseThrow(AuthException::invalidRefreshToken);
        Instant changedAt = user.getCredentialsChangedAt();
        if (changedAt != null && claims.getIssuedAt() != null
                && claims.getIssuedAt().toInstant().isBefore(changedAt.truncatedTo(ChronoUnit.SECONDS))) {
            throw AuthException.invalidRefreshToken();
        }
        return new TokenPairResponse(generateAuthToken(user), generateRefreshToken(user));
    }

    @Override
    public void revokePassword(User user) {
        user.setPasswordHash(unusablePasswordHash());
        user.setCredentialsChangedAt(Instant.now());
    }

    /** BCrypt of 32 random bytes nobody ever sees: the account can't be used with a password until one is reset. */
    private String unusablePasswordHash() {
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        return passwordEncoder.encode(Base64.getEncoder().encodeToString(secret));
    }

    /** "Jane Doe Smith" → ["Jane", "Doe Smith"]; a single word leaves the last name empty. */
    public static String[] splitFullName(String fullName) {
        String[] names = fullName.trim().split("\\s+", 2);
        return new String[]{names[0], names.length > 1 ? names[1] : ""};
    }

    /**
     * Deliberately not @Transactional: each save attempt runs in its own transaction, because a failed flush leaves
     * the persistence context unusable and a retry inside the same transaction would fail too.
     * Retries only on a public_id collision; a duplicate email (concurrent sign-up) becomes a 409.
     */
    private User insertWithUniquePublicId(String email, Function<String, User> userForPublicId) {
        for (int attempt = 1; ; attempt++) {
            User user = userForPublicId.apply(generateUniquePublicId());
            try {
                return userRepository.saveAndFlush(user);
            } catch (DataIntegrityViolationException e) {
                String detail = rootMessage(e);
                if (detail.contains("(email)")) {
                    throw new UserAlreadyExistsException(email);
                }
                if (!detail.contains("(public_id)") || attempt >= MAX_PUBLIC_ID_ATTEMPTS) {
                    throw e;
                }
            }
        }
    }

    private String generateUniquePublicId() {
        String candidate;
        int attempts = 0;
        do {
            candidate = publicIdGenerator.generate();
            attempts++;
            if (attempts > 10) {
                throw new IllegalStateException("Unable to generate unique public ID after 10 attempts");
            }
        } while (userRepository.existsByPublicId(candidate));
        return candidate;
    }

    /** PostgreSQL reports the violated columns in the detail, e.g. "Key (public_id)=(ABCD2345) already exists." */
    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        return String.valueOf(root.getMessage());
    }
}
