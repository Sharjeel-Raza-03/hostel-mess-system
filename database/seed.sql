-- Seed data. Run AFTER schema.sql. Safe to re-run.

-- ---------------------------------------------------------------
-- 1 ADMIN user  (password documented separately; BCrypt cost 10)
-- ---------------------------------------------------------------
INSERT INTO users (full_name, email, password_hash, role, room_no)
VALUES ('Hostel Admin',
        'admin@hostel.local',
        '$2a$10$kc8wWRDM/IT8D2nTzo0kIO27OktRuEBL1yELwngJTqdYNZTYukw1C',
        'ADMIN',
        NULL)
ON CONFLICT (email) DO NOTHING;

-- ---------------------------------------------------------------
-- Default settings
-- ---------------------------------------------------------------
INSERT INTO settings (key, value) VALUES
    ('MEAL_RATE_BREAKFAST', '80.00'),
    ('MEAL_RATE_LUNCH',     '150.00'),
    ('MEAL_RATE_DINNER',    '150.00'),
    ('LOW_STOCK_THRESHOLD', '10')
ON CONFLICT (key) DO NOTHING;

-- ---------------------------------------------------------------
-- 10 sample medicines (expiry dates are relative to today so the
-- seed stays valid whenever it is run)
--   AVAILABLE    : Paracetamol, Ibuprofen, Cetirizine, ORS, Diclofenac Gel
--   LOW_STOCK    : Antacid Syrup (qty = threshold), Povidone-Iodine (qty 4)
--   OUT_OF_STOCK : Cough Syrup, Adhesive Bandages
--   EXPIRED      : Loperamide (qty 25, expired 30 days ago)
-- ---------------------------------------------------------------
INSERT INTO medicines (name, category, description, quantity, unit, expiry_date, updated_by)
SELECT m.name, m.category, m.description, m.quantity, m.unit, m.expiry_date,
       (SELECT id FROM users WHERE email = 'admin@hostel.local')
FROM (VALUES
    ('Paracetamol 500mg',       'Fever',        'Fever and mild pain; 1 tablet every 6-8 hours',     120, 'tablets', CURRENT_DATE + 540),
    ('Ibuprofen 400mg',         'Pain relief',  'Headache, toothache, muscle pain; take after food',  80, 'tablets', CURRENT_DATE + 420),
    ('Cetirizine 10mg',         'Allergy',      'Allergic rhinitis and skin allergy',                 60, 'tablets', CURRENT_DATE + 365),
    ('ORS Sachets',             'Digestive',    'Oral rehydration for diarrhea and dehydration',      45, 'sachets', CURRENT_DATE + 300),
    ('Diclofenac Gel',          'Pain relief',  'Topical gel for sprains and muscle aches',           15, 'tubes',   CURRENT_DATE + 480),
    ('Antacid Syrup',           'Digestive',    'Acidity and heartburn relief',                       10, 'bottles', CURRENT_DATE + 250),
    ('Povidone-Iodine Solution','First aid',    'Antiseptic for cuts and minor wounds',                4, 'bottles', CURRENT_DATE + 330),
    ('Cough Syrup',             'Cold & Cough', 'Dry cough relief',                                    0, 'bottles', CURRENT_DATE + 200),
    ('Adhesive Bandages',       'First aid',    'Assorted sizes for small cuts',                       0, 'boxes',   CURRENT_DATE + 700),
    ('Loperamide 2mg',          'Digestive',    'Short-term relief of loose motions',                 25, 'tablets', CURRENT_DATE - 30)
) AS m(name, category, description, quantity, unit, expiry_date)
ON CONFLICT (name, expiry_date) DO NOTHING;
