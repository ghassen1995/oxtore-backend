package com.oxtore.backend.stock.entity;

import com.oxtore.backend.product.entity.CommissionRule;
import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.product.entity.WholesalePriceTier;
import com.oxtore.backend.product.enums.SaleType;
import com.oxtore.backend.product.enums.TransactionMode;
import com.oxtore.backend.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "product_id")
    Product product;

    @ManyToOne @JoinColumn(name = "store_id")
    Store store;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @OneToMany(mappedBy = "stock", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("minQuantity ASC")
    @Builder.Default
    private List<WholesalePriceTier> wholesalePriceTiers = new ArrayList<>();

    @OneToOne(mappedBy = "stock", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private CommissionRule commissionRule;

    @Column(name = "market_price", precision = 15, scale = 2)
    private BigDecimal marketPrice;

    @Column(name = "retail_price", precision = 15, scale = 2)
    private BigDecimal retailPrice;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    SaleType saleType;

    @Enumerated(EnumType.STRING)
    TransactionMode transactionMode;

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
