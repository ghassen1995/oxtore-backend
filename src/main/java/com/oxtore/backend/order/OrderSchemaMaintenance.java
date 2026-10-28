package com.oxtore.backend.order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate generated "notifications_type_check" listing the notification types that existed when it created the
 * table, and ddl-auto=update never alters existing check constraints, so ORDER notifications would be rejected.
 * Dropping it is idempotent; the enum mapping still restricts the values the application writes. Same statement as
 * db/manual/2026-10-28_customer_orders.sql.
 */
@Slf4j
@Component
public class OrderSchemaMaintenance implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public OrderSchemaMaintenance(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE IF EXISTS notifications DROP CONSTRAINT IF EXISTS notifications_type_check");
        log.debug("notifications type check constraint removed (if present)");
    }
}
