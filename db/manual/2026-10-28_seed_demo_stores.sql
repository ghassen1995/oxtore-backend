-- Demo boutiques and products for the marketplace, owned by one existing account.
-- Set owner_email below to the account that should own them (it becomes SUPERVISOR of each boutique),
-- then run: docker exec -i <db container> psql -U oxtore -d oxtore-db < 2026-10-28_seed_demo_stores.sql
-- Rows match what the app creates (stores + store_categories + store_staff, products + stock, retail, direct sale).
-- Products have no images (the app shows its placeholder); add photos from the app if needed.
-- Safe to run more than once: a boutique whose name already exists is skipped with its products.
-- Run db/manual/2026-10-27_seed_categories.sql first so the category chips exist.

DO $$
DECLARE
    owner_email text := 'CHANGE_ME@example.com';
    owner_id    bigint;
    s           record;
    p           record;
    new_store   bigint;
    new_product bigint;
    created     int := 0;
BEGIN
    SELECT id INTO owner_id FROM users WHERE lower(email) = lower(owner_email);
    IF owner_id IS NULL THEN
        RAISE EXCEPTION 'No user with email %; set owner_email at the top of the script', owner_email;
    END IF;

    CREATE TEMP TABLE demo_stores (store_key text, name text, slogan text, categories text[], sector text,
                                   location text, phone text) ON COMMIT DROP;
    INSERT INTO demo_stores VALUES
        ('tech',   'TechZone Plateau',     'Smartphones et accessoires au meilleur prix', ARRAY['electronique', 'informatique'], 'ELECTRONICS',
         'Plateau, Abidjan',          '+2250701020301'),
        ('mode',   'Maison Awa Couture',   'Mode africaine et prêt-à-porter',            ARRAY['mode'],                          'FASHION',
         'Cocody Angré, Abidjan',     '+2250701020302'),
        ('beaute', 'Belle Nature Cosmétiques', 'Soins naturels au karité et à l''huile de coco', ARRAY['beaute'],               'BEAUTY_AND_HEALTH',
         'Marcory Zone 4, Abidjan',   '+2250701020303'),
        ('maison', 'Déco & Maison Yopougon', 'Tout pour la maison',                       ARRAY['maison-jardin', 'bricolage'],   'HOME_AND_GARDEN',
         'Yopougon Siporex, Abidjan', '+2250701020304'),
        ('food',   'Épicerie du Marché',   'Produits frais et locaux',                    ARRAY['alimentaire'],                   'FOOD_AND_BEVERAGE',
         'Treichville, Abidjan',      '+2250701020305'),
        ('sport',  'Sport Plus Riviera',   'Équipez-vous pour gagner',                    ARRAY['sports'],                        'SPORTS_AND_LEISURE',
         'Riviera 2, Abidjan',        '+2250701020306');

    CREATE TEMP TABLE demo_products (store_key text, name text, price numeric, stock int, condition text) ON COMMIT DROP;
    INSERT INTO demo_products VALUES
        ('tech',   'Samsung Galaxy A15 128 Go',         115000, 12, 'NEW'),
        ('tech',   'iPhone 12 64 Go reconditionné',     195000,  4, 'USED'),
        ('tech',   'Écouteurs Bluetooth sans fil',       12500, 30, 'NEW'),
        ('tech',   'Chargeur rapide 20W USB-C',           6000, 45, 'NEW'),
        ('tech',   'Power bank 20000 mAh',               15000, 18, 'NEW'),
        ('tech',   'Ordinateur portable HP 15 Core i5',  325000,  3, 'NEW'),
        ('mode',   'Robe en wax brodée',                 25000, 10, 'NEW'),
        ('mode',   'Chemise homme en bazin',             18000, 14, 'NEW'),
        ('mode',   'Sac à main en cuir',                 22000,  8, 'NEW'),
        ('mode',   'Sandales en cuir artisanales',       12000, 20, 'NEW'),
        ('mode',   'Ensemble boubou femme',              35000,  6, 'NEW'),
        ('beaute', 'Beurre de karité pur 500 g',          4500, 40, 'NEW'),
        ('beaute', 'Huile de coco vierge 250 ml',         3500, 35, 'NEW'),
        ('beaute', 'Savon noir naturel',                  1500, 60, 'NEW'),
        ('beaute', 'Crème hydratante visage',             8000, 25, 'NEW'),
        ('beaute', 'Parfum oriental 100 ml',             17500, 12, 'NEW'),
        ('maison', 'Ventilateur sur pied 16 pouces',     22000,  9, 'NEW'),
        ('maison', 'Marmite en aluminium 10 L',          12000, 15, 'NEW'),
        ('maison', 'Lot de 6 verres',                     5000, 30, 'NEW'),
        ('maison', 'Perceuse visseuse sans fil',         38000,  5, 'NEW'),
        ('maison', 'Rideaux occultants (paire)',         15000, 11, 'NEW'),
        ('food',   'Riz parfumé 25 kg',                  17500, 20, 'NEW'),
        ('food',   'Huile végétale 5 L',                  7500, 25, 'NEW'),
        ('food',   'Attiéké 1 kg',                         800, 80, 'NEW'),
        ('food',   'Café Arabica moulu 500 g',            4000, 30, 'NEW'),
        ('food',   'Jus de bissap 1 L',                   1200, 50, 'NEW'),
        ('sport',  'Ballon de football taille 5',         9000, 25, 'NEW'),
        ('sport',  'Maillot de football',                 10000, 30, 'NEW'),
        ('sport',  'Haltères 2 x 5 kg',                  18000,  8, 'NEW'),
        ('sport',  'Tapis de yoga',                       8500, 15, 'NEW'),
        ('sport',  'Chaussures de running',              28000, 10, 'NEW');

    FOR s IN SELECT * FROM demo_stores LOOP
        IF EXISTS (SELECT 1 FROM stores WHERE store_name = s.name) THEN
            RAISE NOTICE 'Skipping "%": already exists', s.name;
            CONTINUE;
        END IF;

        INSERT INTO stores (store_name, slogan, business_sector, location, phone_number, created_at, updated_at)
        VALUES (s.name, s.slogan, s.sector, s.location, s.phone, now(), now())
        RETURNING id INTO new_store;

        INSERT INTO store_categories (store_id, category_slug, position)
        SELECT new_store, slug, ord - 1 FROM unnest(s.categories) WITH ORDINALITY AS c(slug, ord);

        INSERT INTO store_staff (store_id, user_id, role, added_by, created_at, updated_at)
        VALUES (new_store, owner_id, 'SUPERVISOR', owner_id, now(), now());

        FOR p IN SELECT * FROM demo_products WHERE store_key = s.store_key LOOP
            INSERT INTO products (condition, product_name, store_id, unit_of_sale, status, created_at, updated_at)
            VALUES (p.condition, p.name, new_store, 'PIECE', 'PUBLISHED', now(), now())
            RETURNING id INTO new_product;

            INSERT INTO stock (product_id, store_id, quantity, retail_price, sale_type, transaction_mode,
                               created_at, updated_at)
            VALUES (new_product, new_store, p.stock, p.price, 'RETAIL', 'DIRECT_SALE', now(), now());
        END LOOP;
        created := created + 1;
    END LOOP;

    RAISE NOTICE '% boutique(s) created for %', created, owner_email;
END $$;
