-- Demo boutiques and products, plain SQL to paste in any PostgreSQL client.
-- 1. Replace owner@example.com (step 1) with the email of the account that should own the boutiques.
-- 2. Run everything in one go. Safe to re-run: existing boutiques / products are not duplicated.
-- Run db/manual/2026-10-27_seed_categories.sql first so the category chips exist.

BEGIN;

-- 1. Owner
CREATE TEMP TABLE seed_owner ON COMMIT DROP AS
SELECT id FROM users WHERE lower(email) = lower('owner@example.com');

-- 2. Boutiques
INSERT INTO stores (store_name, slogan, business_sector, location, phone_number, created_at, updated_at)
SELECT v.name, v.slogan, v.sector, v.location, v.phone, now(), now()
FROM (VALUES
    ('TechZone Plateau',         'Smartphones et accessoires au meilleur prix',     'ELECTRONICS',        'Plateau, Abidjan',          '+2250701020301'),
    ('Maison Awa Couture',       'Mode africaine et prêt-à-porter',                 'FASHION',            'Cocody Angré, Abidjan',     '+2250701020302'),
    ('Belle Nature Cosmétiques', 'Soins naturels au karité et à l''huile de coco',  'BEAUTY_AND_HEALTH',  'Marcory Zone 4, Abidjan',   '+2250701020303'),
    ('Déco & Maison Yopougon',   'Tout pour la maison',                             'HOME_AND_GARDEN',    'Yopougon Siporex, Abidjan', '+2250701020304'),
    ('Épicerie du Marché',       'Produits frais et locaux',                        'FOOD_AND_BEVERAGE',  'Treichville, Abidjan',      '+2250701020305'),
    ('Sport Plus Riviera',       'Équipez-vous pour gagner',                        'SPORTS_AND_LEISURE', 'Riviera 2, Abidjan',        '+2250701020306')
) AS v(name, slogan, sector, location, phone)
WHERE EXISTS (SELECT 1 FROM seed_owner)
  AND NOT EXISTS (SELECT 1 FROM stores s WHERE s.store_name = v.name);

-- 3. Boutique categories
INSERT INTO store_categories (store_id, category_slug, position)
SELECT s.id, v.slug, v.position
FROM (VALUES
    ('TechZone Plateau',         'electronique',  0),
    ('TechZone Plateau',         'informatique',  1),
    ('Maison Awa Couture',       'mode',          0),
    ('Belle Nature Cosmétiques', 'beaute',        0),
    ('Déco & Maison Yopougon',   'maison-jardin', 0),
    ('Déco & Maison Yopougon',   'bricolage',     1),
    ('Épicerie du Marché',       'alimentaire',   0),
    ('Sport Plus Riviera',       'sports',        0)
) AS v(store_name, slug, position)
JOIN stores s ON s.store_name = v.store_name
WHERE NOT EXISTS (SELECT 1 FROM store_categories c WHERE c.store_id = s.id AND c.position = v.position);

-- 4. Owner becomes SUPERVISOR of each boutique
INSERT INTO store_staff (store_id, user_id, role, added_by, created_at, updated_at)
SELECT s.id, o.id, 'SUPERVISOR', o.id, now(), now()
FROM stores s CROSS JOIN seed_owner o
WHERE s.store_name IN ('TechZone Plateau', 'Maison Awa Couture', 'Belle Nature Cosmétiques',
                       'Déco & Maison Yopougon', 'Épicerie du Marché', 'Sport Plus Riviera')
  AND NOT EXISTS (SELECT 1 FROM store_staff st WHERE st.store_id = s.id AND st.user_id = o.id);

-- 5. Products (price in XOF, initial stock)
CREATE TEMP TABLE seed_products ON COMMIT DROP AS
SELECT * FROM (VALUES
    ('TechZone Plateau',         'Samsung Galaxy A15 128 Go',          115000, 12, 'NEW'),
    ('TechZone Plateau',         'iPhone 12 64 Go reconditionné',      195000,  4, 'USED'),
    ('TechZone Plateau',         'Écouteurs Bluetooth sans fil',        12500, 30, 'NEW'),
    ('TechZone Plateau',         'Chargeur rapide 20W USB-C',            6000, 45, 'NEW'),
    ('TechZone Plateau',         'Power bank 20000 mAh',                15000, 18, 'NEW'),
    ('TechZone Plateau',         'Ordinateur portable HP 15 Core i5',  325000,  3, 'NEW'),
    ('Maison Awa Couture',       'Robe en wax brodée',                  25000, 10, 'NEW'),
    ('Maison Awa Couture',       'Chemise homme en bazin',              18000, 14, 'NEW'),
    ('Maison Awa Couture',       'Sac à main en cuir',                  22000,  8, 'NEW'),
    ('Maison Awa Couture',       'Sandales en cuir artisanales',        12000, 20, 'NEW'),
    ('Maison Awa Couture',       'Ensemble boubou femme',               35000,  6, 'NEW'),
    ('Belle Nature Cosmétiques', 'Beurre de karité pur 500 g',           4500, 40, 'NEW'),
    ('Belle Nature Cosmétiques', 'Huile de coco vierge 250 ml',          3500, 35, 'NEW'),
    ('Belle Nature Cosmétiques', 'Savon noir naturel',                   1500, 60, 'NEW'),
    ('Belle Nature Cosmétiques', 'Crème hydratante visage',              8000, 25, 'NEW'),
    ('Belle Nature Cosmétiques', 'Parfum oriental 100 ml',              17500, 12, 'NEW'),
    ('Déco & Maison Yopougon',   'Ventilateur sur pied 16 pouces',      22000,  9, 'NEW'),
    ('Déco & Maison Yopougon',   'Marmite en aluminium 10 L',           12000, 15, 'NEW'),
    ('Déco & Maison Yopougon',   'Lot de 6 verres',                      5000, 30, 'NEW'),
    ('Déco & Maison Yopougon',   'Perceuse visseuse sans fil',          38000,  5, 'NEW'),
    ('Déco & Maison Yopougon',   'Rideaux occultants (paire)',          15000, 11, 'NEW'),
    ('Épicerie du Marché',       'Riz parfumé 25 kg',                   17500, 20, 'NEW'),
    ('Épicerie du Marché',       'Huile végétale 5 L',                   7500, 25, 'NEW'),
    ('Épicerie du Marché',       'Attiéké 1 kg',                          800, 80, 'NEW'),
    ('Épicerie du Marché',       'Café Arabica moulu 500 g',             4000, 30, 'NEW'),
    ('Épicerie du Marché',       'Jus de bissap 1 L',                    1200, 50, 'NEW'),
    ('Sport Plus Riviera',       'Ballon de football taille 5',          9000, 25, 'NEW'),
    ('Sport Plus Riviera',       'Maillot de football',                 10000, 30, 'NEW'),
    ('Sport Plus Riviera',       'Haltères 2 x 5 kg',                   18000,  8, 'NEW'),
    ('Sport Plus Riviera',       'Tapis de yoga',                        8500, 15, 'NEW'),
    ('Sport Plus Riviera',       'Chaussures de running',               28000, 10, 'NEW')
) AS v(store_name, name, price, quantity, condition);

INSERT INTO products (condition, product_name, store_id, unit_of_sale, status, created_at, updated_at)
SELECT v.condition, v.name, s.id, 'PIECE', 'PUBLISHED', now(), now()
FROM seed_products v
JOIN stores s ON s.store_name = v.store_name
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.store_id = s.id AND p.product_name = v.name);

-- 6. Stock: retail, direct sale
INSERT INTO stock (product_id, store_id, quantity, retail_price, sale_type, transaction_mode, created_at, updated_at)
SELECT p.id, s.id, v.quantity, v.price, 'RETAIL', 'DIRECT_SALE', now(), now()
FROM seed_products v
JOIN stores s ON s.store_name = v.store_name
JOIN products p ON p.store_id = s.id AND p.product_name = v.name
WHERE NOT EXISTS (SELECT 1 FROM stock k WHERE k.product_id = p.id AND k.store_id = s.id);

-- Check: should list 6 boutiques with their product counts. Empty = the email in step 1 matched no user.
SELECT s.id, s.store_name, count(p.id) AS products
FROM stores s
JOIN store_staff st ON st.store_id = s.id AND st.user_id IN (SELECT id FROM seed_owner)
LEFT JOIN products p ON p.store_id = s.id
GROUP BY s.id, s.store_name
ORDER BY s.id;

COMMIT;
