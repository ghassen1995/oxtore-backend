package com.oxtore.backend.identity.controller;

import com.oxtore.backend.identity.DTO.UpdateMeRequest;
import com.oxtore.backend.identity.DTO.UpdateSettingsRequest;
import com.oxtore.backend.identity.DTO.UserResponse;
import com.oxtore.backend.identity.DTO.UserSettingsResponse;
import com.oxtore.backend.identity.service.IdentityService;
import jakarta.validation.Valid;
import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.mapper.UserResponseMapper;
import com.oxtore.backend.identity.repository.IdentityRepository;
import com.oxtore.backend.staff.repository.StoreStaffRepository;
import com.oxtore.backend.store.service.StoreService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final IdentityRepository userRepository;
    private final UserResponseMapper userResponseMapper;
    private final StoreStaffRepository storeStaffRepository;
    private final IdentityService identityService;

    @PostMapping("/me/active-boutique/{boutiqueId}")
    public ResponseEntity<UserResponse> setActiveBoutique(
            @PathVariable Long boutiqueId, HttpServletRequest request) {

        Long userId = Long.valueOf(request.getAttribute("userId").toString());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        boolean isStaffThere = storeStaffRepository.findByStoreIdAndUserId(boutiqueId, userId).isPresent();
        if (!isStaffThere) {
            throw new AccessDeniedException("Not staff of store " + boutiqueId);
        }

        user.setActiveStoreId(boutiqueId);
        userRepository.save(user);

        return ResponseEntity.ok(userResponseMapper.toResponse(user));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(HttpServletRequest request) {
        Long userId = Long.valueOf(request.getAttribute("userId").toString());
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        return ResponseEntity.ok(userResponseMapper.toResponse(user));
    }

    /** Edit profile: fullName, phone, avatar (uploaded URL). Omitted fields are unchanged; the email can't change here. */
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@Valid @RequestBody UpdateMeRequest body, HttpServletRequest request) {
        Long userId = Long.valueOf(request.getAttribute("userId").toString());
        User user = identityService.updateMe(userId, body.fullName(), body.phone(), body.avatar());
        return ResponseEntity.ok(userResponseMapper.toResponse(user));
    }

    @GetMapping("/me/settings")
    public ResponseEntity<UserSettingsResponse> getSettings(HttpServletRequest request) {
        Long userId = Long.valueOf(request.getAttribute("userId").toString());
        return ResponseEntity.ok(identityService.getSettings(userId));
    }

    @PatchMapping("/me/settings")
    public ResponseEntity<UserSettingsResponse> updateSettings(@Valid @RequestBody UpdateSettingsRequest body,
                                                               HttpServletRequest request) {
        Long userId = Long.valueOf(request.getAttribute("userId").toString());
        return ResponseEntity.ok(identityService.updateSettings(userId, body));
    }
}