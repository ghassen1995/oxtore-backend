package com.oxtore.backend.stock.entity;

import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.stock.enums.SupplyRequestStatus;
import com.oxtore.backend.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplyRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @ManyToOne @JoinColumn(name = "source_store_id", nullable = false, updatable = false)
    private Store sourceStore;

    @ManyToOne @JoinColumn(name = "reseller_store_id", nullable = false, updatable = false)
    private Store resellerStore;

    @Column(name = "requested_quantity", nullable = false, updatable = false)
    private Integer requestedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplyRequestStatus status;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private Long requestedBy;

    @Column(name = "decided_by")
    private Long decidedBy;

    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "decided_at", updatable = false)
    private LocalDateTime decidedAt;

    @Column(name = "confirmed_at", updatable = false)
    private LocalDateTime confirmedAt;

    @PrePersist
    private void prePersist() {
        this.requestedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        if (this.status == SupplyRequestStatus.APPROVED || this.status == SupplyRequestStatus.DECLINED) {
            this.decidedAt = LocalDateTime.now();
        } else if (this.status == SupplyRequestStatus.CONFIRMED) {
            this.confirmedAt = LocalDateTime.now();
        }
    }
}
