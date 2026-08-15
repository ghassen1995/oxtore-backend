package com.oxtore.backend.product.DTOs;

import java.math.BigDecimal;

public record CommissionRuleRequest(
        BigDecimal levelOneCommission,
        BigDecimal levelTwoCommission,
        BigDecimal levelThreeCommission) {
}
