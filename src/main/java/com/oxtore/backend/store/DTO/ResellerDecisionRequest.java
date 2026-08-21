package com.oxtore.backend.store.DTO;

import jakarta.validation.constraints.NotNull;

public record ResellerDecisionRequest(
        @NotNull Boolean accept
) {}