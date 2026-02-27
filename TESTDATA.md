

USE CENTRAL_KITCHEN_MANGEMENT;

INSERT INTO franchise_store (
store_id,
store_name,
address,
dept_status,
district,
ward,
revenue,
number_of_contact
) VALUES
(FR_001, 'Central Kitchen D1', '123 Nguyen Hue', 'NO_DEBT', 'District 1', 'Ben Nghe', 150000000, '0901234567'),

(FR_002, 'Central Kitchen D7', '456 Nguyen Van Linh', 'IN_DEBT', 'District 7', 'Tan Phong', 80000000, '0912345678');

INSERT INTO order_detail (
order_detail_id,
amount,
order_id_fk,
store_id_fk,
supply_coordinator_id_fk,
note
)
VALUES
('OD_001_01', 150000, 'ORD_001', 'FR_001', '71f746a7-1390-11f1-8701-02504a7d66e6', 'First delivery batch'),

('OD_001_02', 220000, 'ORD_001', 'FR_002', '71f746a7-1390-11f1-8701-02504a7d66e6', 'Second delivery batch');

INSERT INTO orders (
order_id,
priority_level,
note,
order_date,
status_order,
store_id_fk
)
VALUES (
'ORD_001',
2,
'Test order for MySQL',
CURRENT_DATE,
'PENDING',
'71f746a7-1390-11f1-8701-02504a7d66e6'
);


INSERT INTO recipe (
recipe_id,
cooking_time,
cooking_temperature,
published_date,
version,
material_usage_standard
)
VALUES (
'RE_CH_000001',
40,
180.0,
CURDATE(),
'v1.0',
'Standard cheese chicken ingredients'
);

INSERT INTO central_food_category (central_food_type_id, central_food_type_name)
VALUES
('CE_CH_482917', 'Chicken'),

('CE_NO_739204', 'Noodle'),

('CE_CA_156893', 'Cake'),

('CE_BU_904561', 'Burger');

INSERT INTO central_foods (
central_food_id,
food_name,
amount,
expiry_date,
manufacturing_date,
central_food_status,
unit_price_food,
food_height,
food_length,
food_weight,
food_width
)
VALUES
('CE_CH_FO_130001', 'Honey Chicken', 60, '2026-04-01', '2026-02-15', 'AVAILABLE', 6500, 6, 22, 550, 12),

('CE_CH_FO_130002', 'Teriyaki Chicken', 45, '2026-03-25', '2026-02-10', 'AVAILABLE', 7000, 7, 24, 600, 14),

('CE_CH_FO_130003', 'Grilled Chicken', 80, '2026-05-10', '2026-02-20', 'AVAILABLE', 7200, 8, 26, 750, 15);

