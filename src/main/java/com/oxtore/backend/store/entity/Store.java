package com.oxtore.backend.store.entity;

import com.oxtore.backend.store.enums.BusinessSector;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;

    @Column(name = "slogan", length = 150)
    private String slogan;

    /** Legacy single sector, kept in sync with the first category (column is NOT NULL). Prefer {@link #categories}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "business_sector", nullable = false)
    private BusinessSector businessSector;

    /** Category slugs chosen at creation (same list as products / marketplace: "mode", "electronique"...). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "store_categories", joinColumns = @JoinColumn(name = "store_id"))
    @Column(name = "category_slug", nullable = false, length = 50)
    @OrderColumn(name = "position")
    @Builder.Default
    private List<String> categories = new ArrayList<>();

    @Column(name = "location", nullable = false, length = 150)
    private String location;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "email", nullable = true, length = 100)
    private String email;

    /** Storage key ("boutiques/{uuid}.ext") of the logo uploaded via /api/uploads/images; URL built in responses. */
    @Column(name = "logo", length = 255)
    private String logo;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}