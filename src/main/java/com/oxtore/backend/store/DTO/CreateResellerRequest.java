package com.oxtore.backend.store.DTO;

import com.oxtore.backend.common.security.HasStoreId;
import jakarta.validation.constraints.NotNull;

public record CreateResellerRequest(
        @NotNull Long resellerStoreId
) implements HasStoreId {
    @Override
    public Long resolveStoreId() {
        return resellerStoreId;
    }
}