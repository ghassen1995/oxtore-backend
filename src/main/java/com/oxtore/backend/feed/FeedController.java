package com.oxtore.backend.feed;

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

    private final FeedService feedService;

    /** Not paginated yet: nextCursor is always null. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> feed(HttpServletRequest http) {
        List<Map<String, Object>> data = feedService.feed(StoreAccess.currentUserId(http));
        Map<String, Object> body = new HashMap<>();
        body.put("data", data);
        body.put("nextCursor", null);
        return ResponseEntity.ok(body);
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
