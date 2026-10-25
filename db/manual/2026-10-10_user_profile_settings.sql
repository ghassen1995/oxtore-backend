-- Profile: real creation date and per-user app settings.
-- With spring.jpa.hibernate.ddl-auto=update Hibernate applies the equivalent. Run by hand only where ddl-auto is off.

BEGIN;

-- Existing rows get the migration time as their creation date (their real date was never recorded).
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at timestamp(6) with time zone NOT NULL DEFAULT now();

-- NULL language/currency = never saved: the app keeps the device's choice.
ALTER TABLE users ADD COLUMN IF NOT EXISTS settings_language varchar(2);
ALTER TABLE users ADD COLUMN IF NOT EXISTS settings_currency varchar(3);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_notifications boolean NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN IF NOT EXISTS push_notifications boolean NOT NULL DEFAULT true;

COMMIT;
