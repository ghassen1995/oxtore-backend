package com.oxtore.backend.feed;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** A user's like on a product (feed heart). One row per (product, user). */
@Entity
@Table(name = "product_likes",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_likes_product_user", columnNames = {"product_id", "user_id"}),
        indexes = @Index(name = "idx_product_likes_user", columnList = "user_id"))
@Getter
@NoArgsConstructor
public class ProductLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ProductLike(Long productId, Long userId) {
        this.productId = productId;
        this.userId = userId;
        this.createdAt = Instant.now();
    }
}
