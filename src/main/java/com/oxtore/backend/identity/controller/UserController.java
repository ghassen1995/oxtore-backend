package com.oxtore.backend.identity.controller;

import com.oxtore.backend.identity.DTO.CreateUserRequest;
import com.oxtore.backend.identity.DTO.CreateUserResponse;
import com.oxtore.backend.identity.DTO.LoginResponse;
import com.oxtore.backend.identity.DTO.LoginRequest;
import com.oxtore.backend.identity.DTO.ApiError;
import com.oxtore.backend.identity.command.LoginCommand;
import com.oxtore.backend.identity.command.UserCommand;
import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.service.IdentityService;
import com.oxtore.backend.identity.service.IdentityServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class UserController {

    private final IdentityService identityService;

    public UserController(IdentityService identityService) {
       this.identityService = identityService;
    }

    @PostMapping("/register")
    public ResponseEntity<CreateUserResponse> register(@RequestBody CreateUserRequest request) {
        UserCommand command = new UserCommand(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.phoneNumber()
        );
        User saved = identityService.createUser(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new CreateUserResponse(
                        saved.getId(),
                        saved.getEmail(),
                        saved.getFirstName(),
                        saved.getLastName(),
                        saved.getPhoneNumber()
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginCommand command = new LoginCommand(request.email(), request.password());
        User user = identityService.authenticateUser(command);
        String token = ((IdentityServiceImpl) identityService).generateAuthToken(user);
        return ResponseEntity.ok(new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                token,
                "Authentication succeeded"
        ));
    }

    public record ProfileResponse(Long id, String email, String role) {}

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("email");
        String role = (String) request.getAttribute("role");

        if (userId == null || email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("UNAUTHORIZED", "Invalid or missing token", 401));
        }

        return ResponseEntity.ok(new ProfileResponse(userId, email, role));
    }
}