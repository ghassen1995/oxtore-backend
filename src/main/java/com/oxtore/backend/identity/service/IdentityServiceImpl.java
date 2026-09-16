package com.oxtore.backend.identity.service;

import com.oxtore.backend.identity.command.LoginCommand;
import com.oxtore.backend.identity.command.UserCommand;
import com.oxtore.backend.identity.config.JwtUtil;
import com.oxtore.backend.identity.config.PublicIdGenerator;
import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.enums.UserRole;
import com.oxtore.backend.identity.exceptions.AuthenticationFailedException;
import com.oxtore.backend.identity.exceptions.UserAlreadyExistsException;
import com.oxtore.backend.identity.repository.IdentityRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class IdentityServiceImpl implements IdentityService {

    private final IdentityRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PublicIdGenerator publicIdGenerator;

    public IdentityServiceImpl(IdentityRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               JwtUtil jwtUtil,
                               PublicIdGenerator publicIdGenerator ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.publicIdGenerator = publicIdGenerator;
    }

    @Override
    public User createUser(UserCommand command) {
        String email = command.email();
        String publicId = generateUniquePublicId();

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(email);
        }

        String hashedPassword = passwordEncoder.encode(command.password());
        String passwordHash = hashedPassword;

        // role is always USER at registration — store-scoped roles live in staff-service
        User user = new User(publicId, email, passwordHash, command.firstName(),
                command.lastName(), command.phoneNumber(), UserRole.USER);
        return userRepository.save(user);
    }

    @Override
    public User authenticateUser(LoginCommand command) {
        User user = userRepository.findByEmail(command.email()).orElseThrow(
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

    public String generateAuthToken(User user) {
        return jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
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

}
