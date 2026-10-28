-- Reseller settlements: payments a reseller makes to its source store.
-- Hibernate (ddl-auto=update) creates the table on startup; this script is for applying it by hand.

CREATE TABLE IF NOT EXISTS reseller_payments (
    id                BIGSERIAL PRIMARY KEY,
    source_store_id   BIGINT         NOT NULL REFERENCES stores (id),
    reseller_store_id BIGINT         NOT NULL REFERENCES stores (id),
    amount            NUMERIC(15, 2) NOT NULL,
    note              VARCHAR(500),
    recorded_by       BIGINT,
    paid_at           TIMESTAMP      NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_reseller_payments_source_reseller
    ON reseller_payments (source_store_id, reseller_store_id);

-- Backfill: sales a reseller made of another store's products were saved as SETTLED although nothing was paid.
-- Mark them UNSETTLED; recording a payment then settles them oldest first.
UPDATE stock_activity a
SET settlement_status = 'UNSETTLED'
FROM stock s
JOIN products p ON p.id = s.product_id
WHERE a.stock_id = s.id
  AND p.store_id IS NOT NULL
  AND p.store_id <> s.store_id
  AND a.settlement_status = 'SETTLED'
  AND NOT EXISTS (SELECT 1 FROM reseller_payments r
                  WHERE r.source_store_id = p.store_id AND r.reseller_store_id = s.store_id);

-- PAYMENT notifications: the type check constraint (if Hibernate created one) predates them.
ALTER TABLE IF EXISTS notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
