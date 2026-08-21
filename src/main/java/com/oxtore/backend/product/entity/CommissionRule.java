package com.oxtore.backend.product.entity;

import com.oxtore.backend.stock.entity.Stock;
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
    @JoinColumn(name = "stock_id", nullable = false, unique = true)
    private Stock stock;

    @Column(name = "level_one_commission")
    private BigDecimal levelOneCommission;

    @Column(name = "level_two_commission")
    private BigDecimal levelTwoCommission;

    @Column(name = "level_three_commission")
    private BigDecimal levelThreeCommission;
}
