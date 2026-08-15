package com.oxtore.backend.product.DTOs;

import java.math.BigDecimal;

public record CommissionRuleResponse(
        Long id,
        BigDecimal levelOneCommission,
        BigDecimal levelTwoCommission,
        BigDecimal levelThreeCommission
) {
}
