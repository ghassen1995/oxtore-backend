package com.oxtore.backend.product.entity;

import com.oxtore.backend.product.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false)
    private ProductCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false)
    private SaleType saleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_mode", nullable = false)
    private TransactionMode transactionMode;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @Column(name = "market_price", precision = 15, scale = 2)
    private BigDecimal marketPrice;

    @Column(name = "retail_price", precision = 15, scale = 2)
    private BigDecimal retailPrice;

    @Column(name = "available_stock", nullable = true)
    private Integer availableStock;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_of_sale", nullable = false)
    private UnitOfSale unitOfSale;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("minQuantity ASC")
    @Builder.Default
    private List<WholesalePriceTier> wholesalePriceTiers = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private CommissionRule commissionRule;

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