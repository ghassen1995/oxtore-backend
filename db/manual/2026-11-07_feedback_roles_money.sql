-- Client feedback 2026-11 (roles, money precision, "both" sale type). Idempotent.
-- FeedbackSchemaMaintenance runs the same statements at startup; this file is for running them by hand.

-- 1. Boutique creators were added as SUPERVISOR; they are the boutique's MANAGER ("Gestionnaire").
--    Only the creation row (a user added by themselves) is changed, so a SUPERVISOR ("Responsable des ventes")
--    appointed later by a manager is kept.
UPDATE store_staff SET role = 'MANAGER' WHERE role = 'SUPERVISOR' AND added_by = user_id;

-- 2. Hibernate's check constraint on stock.sale_type predates BOTH ("Les deux").
ALTER TABLE IF EXISTS stock DROP CONSTRAINT IF EXISTS stock_sale_type_check;

-- 3. Money in major units with 3 decimals (TND millimes). Widening never loses data.
ALTER TABLE stock                ALTER COLUMN retail_price TYPE NUMERIC(15, 3);
ALTER TABLE stock                ALTER COLUMN market_price TYPE NUMERIC(15, 3);
ALTER TABLE wholesale_price_tiers ALTER COLUMN unit_price  TYPE NUMERIC(15, 3);
ALTER TABLE stock_activity       ALTER COLUMN unit_price   TYPE NUMERIC(15, 3);
ALTER TABLE customer_orders      ALTER COLUMN subtotal     TYPE NUMERIC(15, 3);
ALTER TABLE customer_orders      ALTER COLUMN delivery_fee TYPE NUMERIC(15, 3);
ALTER TABLE customer_orders      ALTER COLUMN total        TYPE NUMERIC(15, 3);
ALTER TABLE customer_order_items ALTER COLUMN unit_price   TYPE NUMERIC(15, 3);
ALTER TABLE customer_order_items ALTER COLUMN line_total   TYPE NUMERIC(15, 3);
ALTER TABLE reseller_payments    ALTER COLUMN amount       TYPE NUMERIC(15, 3);

-- New columns (created by Hibernate ddl-auto=update; listed here for databases managed by hand):
--   store_staff.active boolean NOT NULL DEFAULT true
--   stores.currency varchar(3), stores.language varchar(2), stores.show_phone boolean NOT NULL DEFAULT true
--   reseller_relation.message varchar(500), reseller_relation.rejection_reason varchar(255)
--   store_follows (id, store_id, user_id, created_at, UNIQUE (store_id, user_id))
