package com.oxtore.backend.store.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** A user following a boutique ("Suivre la boutique"). */
@Entity
@Table(name = "store_follows", uniqueConstraints = @UniqueConstraint(
        name = "uq_store_follow", columnNames = {"store_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
