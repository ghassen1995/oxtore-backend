package com.oxtore.backend.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** App configuration readable before sign-in (see SecurityConfig). */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final CurrencyCatalog currencyCatalog;

    /** [{code, label, symbol, decimals, deliveryFee}], TND included. */
    @GetMapping("/currencies")
    public ResponseEntity<List<CurrencyCatalog.Currency>> currencies() {
        return ResponseEntity.ok(currencyCatalog.all());
    }
}
