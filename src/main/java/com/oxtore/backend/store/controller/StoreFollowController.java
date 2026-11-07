package com.oxtore.backend.store.controller;

import com.oxtore.backend.common.error.ApiException;
import com.oxtore.backend.common.security.StoreAccess;
import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.store.entity.StoreFollow;
import com.oxtore.backend.store.repository.StoreFollowRepository;
import com.oxtore.backend.store.repository.StoreRepository;
import com.oxtore.backend.upload.ImageUrlResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Follow / unfollow a boutique; both are idempotent and answer 204. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StoreFollowController {

    private final StoreFollowRepository followRepository;
    private final StoreRepository storeRepository;
    private final ImageUrlResolver imageUrlResolver;

    @Transactional
    @PostMapping("/boutiques/{id}/follow")
    public ResponseEntity<Void> follow(@PathVariable("id") Long storeId, HttpServletRequest http) {
        Long userId = StoreAccess.currentUserId(http);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> ApiException.notFound("BOUTIQUE_NOT_FOUND", "Boutique not found"));
        if (!followRepository.existsByStore_IdAndUserId(storeId, userId)) {
            try {
                followRepository.saveAndFlush(StoreFollow.builder().store(store).userId(userId).build());
            } catch (DataIntegrityViolationException alreadyFollowing) {
                // concurrent double tap: the other request created it
            }
        }
        return ResponseEntity.noContent().build();
    }

    @Transactional
    @DeleteMapping("/boutiques/{id}/follow")
    public ResponseEntity<Void> unfollow(@PathVariable("id") Long storeId, HttpServletRequest http) {
        followRepository.deleteFollow(storeId, StoreAccess.currentUserId(http));
        return ResponseEntity.noContent().build();
    }

    /** {data: [{id, name, logo}]}, most recently followed first. */
    @Transactional(readOnly = true)
    @GetMapping("/users/me/following")
    public ResponseEntity<Map<String, Object>> following(HttpServletRequest http) {
        List<Map<String, Object>> data = followRepository.findByUserIdWithStore(StoreAccess.currentUserId(http)).stream()
                .map(f -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", f.getStore().getId());
                    m.put("name", f.getStore().getStoreName());
                    m.put("logo", imageUrlResolver.toPublicUrl(f.getStore().getLogo()));
                    return m;
                })
                .toList();
        return ResponseEntity.ok(Map.of("data", data, "meta", Map.of("total", data.size())));
    }
}
