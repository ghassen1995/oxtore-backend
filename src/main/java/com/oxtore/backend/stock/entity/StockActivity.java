package com.oxtore.backend.stock.entity;

import com.oxtore.backend.stock.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_activity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockActivity {
    @Id
    @GeneratedValue
    Long id;

    @ManyToOne
    @JoinColumn(name = "stock_id")
    Stock stock;

    @Column(name = "quantity_sold")
    Integer quantitySold;

    @Column(name = "unit_price")
    BigDecimal unitPrice;

    @Column(name = "sold_by")
    Long soldBy;

    @Enumerated(EnumType.STRING)
    SettlementStatus settlementStatus;

    @Column(name = "sold_at", updatable = false)
    LocalDateTime soldAt;

    @PrePersist
    private void prePersist() {
        this.soldAt = LocalDateTime.now();
    }
}
