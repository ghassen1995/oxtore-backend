package com.oxtore.backend.product.entity;

import com.oxtore.backend.product.enums.ProductResellingRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_reseller")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReseller {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "reseller_user_id", nullable = false)
    private Long resellerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "Product_reselling_request_status", nullable = false)
    private ProductResellingRequestStatus status;

    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at", updatable = false)
    private LocalDateTime approvedAt;

    @Column(name = "approved_by_user_id", updatable = false, nullable = true)
    private Long approvedByUserId;


}
