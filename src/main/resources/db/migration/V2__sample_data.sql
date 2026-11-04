-- Fictional example records, not production data.
INSERT INTO province (id, name, active, deleted) VALUES
(1, 'Bali', TRUE, FALSE), (2, 'Jakarta', TRUE, FALSE), (3, 'Inactive Province', FALSE, FALSE);
INSERT INTO branch (id, name, province_id, active, deleted) VALUES
(1, 'Bali Central', 1, TRUE, FALSE), (2, 'Jakarta Central', 2, TRUE, FALSE),
(3, 'Bali Inactive', 1, FALSE, FALSE), (4, 'Bali Empty', 1, TRUE, FALSE),
(5, 'Hidden Province Branch', 3, TRUE, FALSE);
INSERT INTO store (id, name, branch_id, active, deleted) VALUES
(1, 'Bali Sample One', 1, TRUE, FALSE), (2, 'Bali Sample Two', 1, TRUE, FALSE),
(3, 'Jakarta Sample One', 2, TRUE, FALSE), (4, 'Inactive Store', 1, FALSE, FALSE),
(5, 'Deleted Store', 1, TRUE, TRUE), (6, 'Hidden Branch Store', 3, TRUE, FALSE),
(7, 'Hidden Province Store', 5, TRUE, FALSE);
INSERT INTO whitelist_store (store_id, created_at) VALUES (3, UTC_TIMESTAMP(6));
