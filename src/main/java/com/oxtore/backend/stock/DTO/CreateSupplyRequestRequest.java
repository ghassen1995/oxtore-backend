package com.oxtore.backend.stock.DTO;

import com.oxtore.backend.common.security.HasStoreId;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateSupplyRequestRequest(
        @NotNull Long productId,
        @NotNull Long resellerStoreId,
        @NotNull @Min(1) Integer requestedQuantity
) implements HasStoreId {
    @Override public Long resolveStoreId() { return resellerStoreId; }
}