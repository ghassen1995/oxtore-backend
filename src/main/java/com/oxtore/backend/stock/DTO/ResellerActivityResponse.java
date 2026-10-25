package com.oxtore.backend.stock.DTO;

import java.math.BigDecimal;
import java.util.List;

public record ResellerActivityResponse(
        List<ResellerSummary> resellers,
        List<ResellerSaleLine> activities,
        BigDecimal totalOwedUnsettled
) {}
