-- Marketplace / boutique categories (GET /api/marketplace/categories).
-- slug = what boutiques and products store, and the app's translation key (categories.<slug>).
-- icon = Ionicons name. "all" is the marketplace's "show everything" chip; boutique creation hides it.
-- Safe to run more than once: existing slugs are left as they are.

INSERT INTO categories (slug, name, icon) VALUES
    ('all',           'All',           'apps-outline'),
    ('automobile',    'Automotive',    'car-outline'),
    ('electronique',  'Electronics',   'phone-portrait-outline'),
    ('informatique',  'IT',            'laptop-outline'),
    ('maison-jardin', 'Home & Garden', 'home-outline'),
    ('mode',          'Fashion',       'shirt-outline'),
    ('beaute',        'Beauty',        'fitness-outline'),
    ('enfants',       'Kids',          'balloon-outline'),
    ('sports',        'Sports',        'basketball-outline'),
    ('bricolage',     'DIY',           'construct-outline'),
    ('alimentaire',   'Food',          'basket-outline'),
    ('animaux',       'Pets',          'paw-outline'),
    ('autres',        'Others',        'ellipsis-horizontal-outline')
ON CONFLICT (slug) DO NOTHING;
