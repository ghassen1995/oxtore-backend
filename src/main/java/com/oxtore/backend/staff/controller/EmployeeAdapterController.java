package com.oxtore.backend.staff.controller;

import com.oxtore.backend.common.security.StoreAccess;
import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.repository.IdentityRepository;
import com.oxtore.backend.staff.DTOs.AddEmployeeRequest;
import com.oxtore.backend.staff.DTOs.EmployeeResponse;
import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.staff.mappers.EmployeeMapper;
import com.oxtore.backend.staff.repository.StoreStaffRepository;
import com.oxtore.backend.staff.service.StoreStaffService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** HR of one boutique: only its supervisor and managers may see or change the team. */
@RestController
@RequestMapping("/api/boutiques/{boutiqueId}/employees")
@RequiredArgsConstructor
public class EmployeeAdapterController {

    private final StoreStaffService storeStaffService;
    private final StoreStaffRepository storeStaffRepository;
    private final EmployeeMapper mapper;
    private final IdentityRepository identityRepository;
    private final StoreAccess storeAccess;

    @Transactional(readOnly = true)
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll(@PathVariable("boutiqueId") Long boutiqueId,
                                                      HttpServletRequest http) {
        storeAccess.require(StoreAccess.currentUserId(http), boutiqueId, StoreAccess.MANAGERS);
        List<StoreStaff> staff = storeStaffRepository.findAllByStoreId(boutiqueId); // add if missing: findByStore_Id
        // Load every staff member's user in one query instead of one per row.
        Map<Long, User> users = identityRepository
                .findAllById(staff.stream().map(StoreStaff::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<EmployeeResponse> data = staff.stream()
                .map(s -> toResponse(s, users.get(s.getUserId())))
                .toList();
        return ResponseEntity.ok(Map.of("data", data, "meta", Map.of()));
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> add(@PathVariable("boutiqueId") Long boutiqueId,
                                                @RequestBody AddEmployeeRequest req, HttpServletRequest http) {
        Long addedBy = StoreAccess.currentUserId(http);
        storeAccess.require(addedBy, boutiqueId, StoreAccess.MANAGERS);
        // req.role must map SELLER->SALESMAN
        StaffRole role = "SELLER".equals(req.role()) ? StaffRole.SELLER : StaffRole.valueOf(req.role());
        StoreStaff saved = storeStaffService.addStaff(boutiqueId, req.userPublicId(), role, addedBy);
        User user = identityRepository.findById(saved.getUserId()).orElse(null);
        return ResponseEntity.status(201).body(toResponse(saved, user));
    }

    /** Finds a user by public id to add them; exposes email and phone, so HR rights are required. */
    @GetMapping("/lookup/{publicId}")
    public ResponseEntity<?> lookupUser(@PathVariable("boutiqueId") Long boutiqueId,
                                        @PathVariable("publicId") String publicId, HttpServletRequest http) {
        storeAccess.require(StoreAccess.currentUserId(http), boutiqueId, StoreAccess.MANAGERS);
        return identityRepository.findByPublicId(publicId)
                .map(u -> ResponseEntity.ok(Map.of(
                        "publicId", u.getPublicId(),
                        "fullName", u.getFirstName() + " " + u.getLastName(),
                        "email", u.getEmail(),
                        "phone", u.getPhoneNumber()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /** The mapper only knows StoreStaff; name/email/phone live on the User, so fill them in here. */
    private EmployeeResponse toResponse(StoreStaff staff, User user) {
        EmployeeResponse base = mapper.toResponse(staff);
        if (user == null) return base;
        return new EmployeeResponse(
                base.id(),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                base.avatar(),
                base.role(),
                base.boutiqueId(),
                base.boutiqueName(),
                base.status(),
                base.createdAt());
    }
}
