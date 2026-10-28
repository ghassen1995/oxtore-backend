package com.oxtore.backend.notification.enums;

/** Serialized lowercase (e.g. "stock_request") to match the Angular NotificationType union. */
public enum NotificationType {
    SALE,
    COMMISSION,
    STOCK,
    HR,
    SYSTEM,
    NETWORK,
    STOCK_REQUEST,
    BOUTIQUE_REQUEST,
    ORDER;

    public String apiValue() {
        return name().toLowerCase();
    }
}
