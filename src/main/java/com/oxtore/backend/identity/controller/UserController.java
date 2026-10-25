package com.oxtore.backend.identity.controller;

import com.oxtore.backend.identity.DTO.*;
import com.oxtore.backend.identity.command.LoginCommand;
import com.oxtore.backend.identity.command.UserCommand;
import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.mapper.UserResponseMapper;
import com.oxtore.backend.identity.otp.OtpService;
import com.oxtore.backend.identity.service.GoogleAuthService;
import com.oxtore.backend.identity.service.IdentityService;
import com.oxtore.backend.identity.service.IdentityServiceImpl;
import com.oxtore.backend.identity.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final IdentityService identityService;
    private final UserResponseMapper userResponseMapper;
    private final OtpService otpService;
    private final PasswordResetService passwordResetService;
    private final GoogleAuthService googleAuthService;

    /** New accounts start unverified; the app then asks for an OTP (POST /otp/send, then /verify-otp). */
    @PostMapping({"/register", "/signup"})
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody SignupRequest request) {
        String[] names = IdentityServiceImpl.splitFullName(request.fullName());

        UserCommand command = new UserCommand(
                OtpService.normalize(request.email()), request.password(), names[0], names[1], request.phone().trim());
        User saved = identityService.createUser(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse(saved));
    }

    /** Unverified users can still log in; the response carries isVerified=false so the app routes them to OTP. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        LoginCommand command = new LoginCommand(OtpService.normalize(request.email()), request.password());
        User user = identityService.authenticateUser(command);

        return ResponseEntity.ok(authResponse(user));
    }

    /** Exchanges a refresh token for a new access + refresh pair. 401 INVALID_REFRESH_TOKEN when it can't. */
    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(identityService.refresh(request.refreshToken()));
    }

    /**
     * Tokens are stateless, so there is nothing to revoke server-side: the app discards them. Kept as an
     * authenticated endpoint so the contract matches the app and a revocation list can be added later.
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    /** Same response whether or not the email belongs to an account waiting for verification. */
    @PostMapping("/otp/send")
    public ResponseEntity<SendOtpResponse> sendOtp(@Valid @RequestBody SendOtpRequest request,
                                                   HttpServletRequest httpRequest) {
        return ResponseEntity.ok(otpService.send(request.email(), request.method(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<MessageResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        otpService.verify(request.email(), request.code());
        return ResponseEntity.ok(new MessageResponse("Email verified successfully"));
    }

    /** Emails a reset code. Same response whether or not the email has an account. */
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                                 HttpServletRequest httpRequest) {
        SendOtpResponse sent = otpService.sendPasswordReset(request.email(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(new ForgotPasswordResponse(
                "If an account exists for this email, a reset code has been sent.", sent.expiresIn(), sent.resendIn()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.email(), request.code(), request.newPassword());
        return ResponseEntity.ok(new MessageResponse("Password reset successfully"));
    }

    /** Sign in (or sign up) with a Google ID token. The user has no phone yet when the account is new. */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> google(@Valid @RequestBody GoogleLoginRequest request) {
        User user = googleAuthService.login(request.idToken());
        return ResponseEntity.ok(authResponse(user));
    }

    @PatchMapping("/complete-profile")
    public ResponseEntity<UserResponse> completeProfile(@Valid @RequestBody CompleteProfileRequest request,
                                                        HttpServletRequest httpRequest) {
        Long userId = Long.valueOf(httpRequest.getAttribute("userId").toString());
        User user = identityService.completeProfile(userId, request);
        return ResponseEntity.ok(userResponseMapper.toResponse(user));
    }

    private AuthResponse authResponse(User user) {
        return new AuthResponse(identityService.generateAuthToken(user), identityService.generateRefreshToken(user),
                userResponseMapper.toResponse(user));
    }
}
