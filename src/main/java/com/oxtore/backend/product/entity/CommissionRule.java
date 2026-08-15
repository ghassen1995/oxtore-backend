package com.oxtore.backend.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "commission_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(name = "level_one_commission")
    private BigDecimal levelOneCommission;

    @Column(name = "level_two_commission")
    private BigDecimal levelTwoCommission;

    @Column(name = "level_three_commission")
    private BigDecimal levelThreeCommission;
}
