package com.oxtore.backend.stock.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ResellerPaymentResponse(
        Long id,
        Long sourceStoreId,
        String sourceStoreName,
        Long resellerStoreId,
        String resellerStoreName,
        BigDecimal amount,
        String note,
        String recordedByName,
        LocalDateTime paidAt
) {}
