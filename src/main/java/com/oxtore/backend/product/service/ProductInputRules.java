package com.oxtore.backend.product.service;

import com.oxtore.backend.common.error.ApiException;
import com.oxtore.backend.product.enums.SaleType;
import com.oxtore.backend.product.enums.TransactionMode;

import java.math.BigDecimal;
import java.util.Locale;

/** Parsing of the article fields the app sends, shared by creation and update. Errors: 400 VALIDATION_ERROR. */
public final class ProductInputRules {

    private ProductInputRules() {}

    /** retail (default) | wholesale | both. */
    public static SaleType parseSaleType(Object raw) {
        if (raw == null) return SaleType.RETAIL;
        return switch (String.valueOf(raw).trim().toLowerCase(Locale.ROOT)) {
            case "retail" -> SaleType.RETAIL;
            case "wholesale" -> SaleType.WHOLESALE;
            case "both" -> SaleType.BOTH;
            default -> throw ApiException.validation("saleType", "saleType must be retail, wholesale or both");
        };
    }

    /** Mandatory: direct (Dropshipping) | consignment (Consignation). */
    public static TransactionMode parseTransactionMode(Object raw) {
        String v = raw == null ? "" : String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
        return switch (v) {
            case "direct" -> TransactionMode.DIRECT_SALE;
            case "consignment" -> TransactionMode.CONSIGNMENT;
            case "" -> throw ApiException.validation("transactionMode",
                    "Choose a selling method: direct (dropshipping) or consignment");
            default -> throw ApiException.validation("transactionMode", "transactionMode must be direct or consignment");
        };
    }

    /** Major units ("32.000" or 32 = 32 DT), never minor units; null when absent and not required. */
    public static BigDecimal money(Object raw, String field, boolean required) {
        if (raw == null || (raw instanceof String s && s.isBlank())) {
            if (required) throw ApiException.validation(field, field + " is required");
            return null;
        }
        BigDecimal value;
        try {
            value = new BigDecimal(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            throw ApiException.validation(field, field + " must be a number");
        }
        if (value.signum() < 0) throw ApiException.validation(field, field + " must not be negative");
        if (value.stripTrailingZeros().scale() > 3) {
            throw ApiException.validation(field, field + " has more than 3 decimals");
        }
        return value;
    }

    public static int wholeNumber(Object raw, String field, int min) {
        int v;
        try {
            v = new BigDecimal(String.valueOf(raw).trim()).intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw ApiException.validation(field, field + " must be a whole number");
        }
        if (v < min) throw ApiException.validation(field, field + " must be at least " + min);
        return v;
    }
}
