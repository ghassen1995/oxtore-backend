package com.oxtore.backend.product.enums;

import java.util.List;

/** "Gros" (WHOLESALE), "Détail" (RETAIL) or "Les deux" (BOTH). */
public enum SaleType {
    WHOLESALE, RETAIL, BOTH;

    /** Offers sold to the public (marketplace, checkout). */
    public static final List<SaleType> SOLD_RETAIL = List.of(RETAIL, BOTH);
    /** Offers sold in bulk to resellers. */
    public static final List<SaleType> SOLD_WHOLESALE = List.of(WHOLESALE, BOTH);

    public boolean isRetail() { return this == RETAIL || this == BOTH; }
    public boolean isWholesale() { return this == WHOLESALE || this == BOTH; }

    /** "retail" / "wholesale" / "both", as the app sends and reads it. */
    public String apiValue() { return name().toLowerCase(); }
}
