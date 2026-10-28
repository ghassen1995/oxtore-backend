package com.oxtore.backend.stock.entity;

import com.oxtore.backend.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cash a reseller handed over to its source store, against what it owes for the source's products it sold. */
@Entity
@Table(name = "reseller_payments", indexes = {
        @Index(name = "idx_reseller_payments_source_reseller", columnList = "source_store_id, reseller_store_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResellerPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_store_id", nullable = false)
    private Store sourceStore;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reseller_store_id", nullable = false)
    private Store resellerStore;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "recorded_by")
    private Long recordedBy;

    @Column(name = "paid_at", nullable = false, updatable = false)
    private LocalDateTime paidAt;

    @PrePersist
    private void prePersist() {
        this.paidAt = LocalDateTime.now();
    }
}
