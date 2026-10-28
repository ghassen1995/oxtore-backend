package com.oxtore.backend.stock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Sales a reseller made of another store's products used to be saved as SETTLED although nothing was paid.
 * Marks them UNSETTLED for reseller / source pairs that have no payment yet, so it never undoes a settlement
 * and is a no-op once applied. Same statement as db/manual/2026-10-28_reseller_payments.sql.
 */
@Slf4j
@Component
public class SettlementBackfill implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public SettlementBackfill(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        int updated = jdbcTemplate.update("""
                UPDATE stock_activity a
                SET settlement_status = 'UNSETTLED'
                FROM stock s
                JOIN products p ON p.id = s.product_id
                WHERE a.stock_id = s.id
                  AND p.store_id IS NOT NULL
                  AND p.store_id <> s.store_id
                  AND a.settlement_status = 'SETTLED'
                  AND NOT EXISTS (SELECT 1 FROM reseller_payments r
                                  WHERE r.source_store_id = p.store_id AND r.reseller_store_id = s.store_id)
                """);
        if (updated > 0) {
            log.info("Settlement backfill: {} reseller sale(s) marked UNSETTLED", updated);
        }
    }
}
