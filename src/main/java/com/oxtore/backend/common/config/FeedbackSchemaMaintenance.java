package com.oxtore.backend.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Idempotent fixes for the 2026-11 client feedback, run at startup (ddl-auto=update can't do them). Same statements
 * as db/manual/2026-11-07_feedback_roles_money.sql.
 * <ol>
 *   <li>Boutique creators used to be added as SUPERVISOR; they are the boutique's MANAGER ("Gestionnaire"). Only rows
 *       a user added for themselves (the creation row, added_by = user_id) are touched, so SUPERVISOR ("Responsable
 *       des ventes") given later by a manager is kept.</li>
 *   <li>Hibernate's generated check constraint on stock.sale_type predates BOTH ("Les deux").</li>
 *   <li>Money columns go to 3 decimals (TND millimes); only columns still at a smaller scale are altered.</li>
 * </ol>
 */
@Slf4j
@Component
public class FeedbackSchemaMaintenance implements ApplicationRunner {

    /** table, column */
    private static final List<String[]> MONEY_COLUMNS = List.of(
            new String[]{"stock", "retail_price"},
            new String[]{"stock", "market_price"},
            new String[]{"wholesale_price_tiers", "unit_price"},
            new String[]{"stock_activity", "unit_price"},
            new String[]{"customer_orders", "subtotal"},
            new String[]{"customer_orders", "delivery_fee"},
            new String[]{"customer_orders", "total"},
            new String[]{"customer_order_items", "unit_price"},
            new String[]{"customer_order_items", "line_total"},
            new String[]{"reseller_payments", "amount"}
    );

    private final JdbcTemplate jdbcTemplate;

    public FeedbackSchemaMaintenance(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        int creators = jdbcTemplate.update(
                "UPDATE store_staff SET role = 'MANAGER' WHERE role = 'SUPERVISOR' AND added_by = user_id");
        if (creators > 0) log.info("Role migration: {} boutique creator(s) changed from SUPERVISOR to MANAGER", creators);

        jdbcTemplate.execute("ALTER TABLE IF EXISTS stock DROP CONSTRAINT IF EXISTS stock_sale_type_check");

        for (String[] c : MONEY_COLUMNS) {
            Integer scale = jdbcTemplate.query(
                    "SELECT numeric_scale FROM information_schema.columns WHERE table_schema = current_schema() "
                            + "AND table_name = ? AND column_name = ?",
                    rs -> rs.next() ? (Integer) rs.getObject(1) : null, c[0], c[1]);
            if (scale != null && scale < 3) {
                jdbcTemplate.execute("ALTER TABLE %s ALTER COLUMN %s TYPE NUMERIC(15, 3)".formatted(c[0], c[1]));
                log.info("Money column {}.{} widened to 3 decimals", c[0], c[1]);
            }
        }
    }
}
