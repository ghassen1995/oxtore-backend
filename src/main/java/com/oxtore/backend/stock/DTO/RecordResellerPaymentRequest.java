package com.oxtore.backend.stock.DTO;

import java.math.BigDecimal;

public record RecordResellerPaymentRequest(
        Long resellerStoreId,
        BigDecimal amount,
        String note
) {}
