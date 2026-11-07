package com.oxtore.backend.feed;

import com.oxtore.backend.common.error.ApiException;
import com.oxtore.backend.common.security.StoreAccess;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feed/products")
@RequiredArgsConstructor
public class FeedController {

    private static final int MAX_LIMIT = 100;

    private final FeedService feedService;

    /**
     * Newest first. Pagination: {@code ?limit=N} returns the first N items and {@code nextCursor}; pass it back as
     * {@code ?cursor=...&limit=N} for the next page. {@code nextCursor} is null on the last page. Without
     * {@code limit} the whole feed is returned (nextCursor null).
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> feed(@RequestParam(required = false) String cursor,
                                                    @RequestParam(required = false) Integer limit,
                                                    HttpServletRequest http) {
        List<Map<String, Object>> all = feedService.feed(StoreAccess.currentUserId(http));

        int offset = 0;
        if (cursor != null && !cursor.isBlank()) {
            try {
                offset = Integer.parseInt(cursor.trim());
            } catch (NumberFormatException e) {
                throw ApiException.validation("cursor", "Invalid cursor");
            }
            if (offset < 0) throw ApiException.validation("cursor", "Invalid cursor");
        }
        int from = Math.min(offset, all.size());
        int to = limit == null ? all.size() : Math.min(all.size(), from + Math.max(1, Math.min(limit, MAX_LIMIT)));

        Map<String, Object> body = new HashMap<>();
        body.put("data", all.subList(from, to));
        body.put("nextCursor", to < all.size() ? String.valueOf(to) : null);
        return ResponseEntity.ok(body);
    }

    /** The user's favourites: {data: [feed items]}. */
    @GetMapping("/liked")
    public ResponseEntity<Map<String, Object>> liked(HttpServletRequest http) {
        return ResponseEntity.ok(Map.of("data", feedService.liked(StoreAccess.currentUserId(http)), "meta", Map.of()));
    }

    /** Toggles the current user's like: {liked, likes}. */
    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable("id") Long productId, HttpServletRequest http) {
        return feedService.toggleLike(productId, StoreAccess.currentUserId(http))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("code", "PRODUCT_NOT_FOUND", "message", "Product not found", "status", 404)));
    }
}
