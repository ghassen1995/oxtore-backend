package com.oxtore.backend.common.config;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Currencies the app supports (same list as the app's currency.constants.ts), served by GET /api/config/currencies.
 * Amounts are always major units ({@code 32.000} TND = 32 DT); {@code decimals} is only for display and rounding.
 * {@code deliveryFee} is the flat fee per boutique order, used by checkout instead of the client's estimate.
 */
@Component
public class CurrencyCatalog {

    /** Boutiques created before the currency was stored have always been reported in this currency. */
    public static final String DEFAULT_CURRENCY = "TND";

    public record Currency(String code, String label, String symbol, int decimals, BigDecimal deliveryFee) {}

    private static final List<Currency> CURRENCIES = List.of(
            new Currency("XOF", "Franc CFA (FCFA)", "FCFA", 0, new BigDecimal("1500")),
            new Currency("EUR", "Euro", "€", 2, new BigDecimal("3")),
            new Currency("USD", "Dollar US", "$", 2, new BigDecimal("3")),
            new Currency("GNF", "Franc Guinéen", "GNF", 0, new BigDecimal("25000")),
            new Currency("MAD", "Dirham Marocain", "MAD", 2, new BigDecimal("30")),
            new Currency("TND", "Dinar Tunisien", "DT", 3, new BigDecimal("7"))
    );

    public List<Currency> all() {
        return CURRENCIES;
    }

    public Optional<Currency> find(String code) {
        if (code == null) return Optional.empty();
        return CURRENCIES.stream().filter(c -> c.code().equalsIgnoreCase(code.trim())).findFirst();
    }

    public boolean isSupported(String code) {
        return find(code).isPresent();
    }

    /** The boutique's currency, or the default for boutiques that never stored one. */
    public static String orDefault(String code) {
        return code == null || code.isBlank() ? DEFAULT_CURRENCY : code;
    }
}
