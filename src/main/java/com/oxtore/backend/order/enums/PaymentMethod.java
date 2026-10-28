package com.oxtore.backend.order.enums;

import java.util.Locale;

/** Only cash on delivery for now. Serialized lowercase ("cash_on_delivery"). */
public enum PaymentMethod {
    CASH_ON_DELIVERY;

    public String apiValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Missing means cash on delivery; "cash" is accepted as an alias. */
    public static PaymentMethod fromApi(String value) {
        if (value == null || value.isBlank()) return CASH_ON_DELIVERY;
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (v.equals("cash") || v.equals("cash_on_delivery")) return CASH_ON_DELIVERY;
        throw new IllegalArgumentException("Unsupported payment method: " + value);
    }
}
