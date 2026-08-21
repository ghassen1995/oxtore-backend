package com.oxtore.backend.stock.DTO;

import jakarta.validation.constraints.NotNull;

public record SupplyDecisionRequest(
        @NotNull Boolean accept
) {}