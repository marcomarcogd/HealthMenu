INSERT INTO customer (id, name, nickname, gender, phone, exclusive_title, note, status, create_time, update_time, deleted)
VALUES
    (2001, '张三', '张女士', '女', '13800000001', '张女士的营养调理餐单', '示例客户，可直接进入餐单生成流程', 1, NOW(), NOW(), 0),
    (2002, '李四', '李先生', '男', '13800000002', '李先生的体重管理餐单', '示例客户，用于演示模板切换', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO menu_template_section (id, template_id, section_type, title, sort_order, enabled, style_config_json, allow_image, create_time, update_time, deleted)
VALUES
    (3001, 1001, 'EXCLUSIVE_TITLE', '专属标题', 1, 1, '{"allowBold":true,"allowColor":true}', 1, NOW(), NOW(), 0),
    (3002, 1001, 'SWAP_GUIDE', '核心食物互换指南', 2, 1, '{"allowBold":true,"allowColor":true}', 1, NOW(), NOW(), 0),
    (3003, 1001, 'WEEKLY_TIP', '每周提示', 3, 1, '{"allowBold":true,"allowColor":true}', 1, NOW(), NOW(), 0),
    (3004, 1001, 'DAILY_MENU', '每日餐单', 4, 1, '{"allowBold":true,"allowColor":true}', 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO menu_template_meal (id, template_id, meal_code, meal_name, time_label, sort_order, enabled, create_time, update_time, deleted)
VALUES
    (4001, 1001, 'breakfast', '早餐', '早餐时间', 1, 1, NOW(), NOW(), 0),
    (4002, 1001, 'morning_snack', '上午加餐', '上午加餐时间', 2, 1, NOW(), NOW(), 0),
    (4003, 1001, 'lunch', '午餐', '午餐时间', 3, 1, NOW(), NOW(), 0),
    (4004, 1001, 'afternoon_snack', '下午加餐', '下午加餐时间', 4, 1, NOW(), NOW(), 0),
    (4005, 1001, 'dinner', '晚餐', '晚餐时间', 5, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO menu_template_meal_item (id, template_meal_id, item_code, item_name, content_format, sort_order, enabled, allow_image, create_time, update_time, deleted)
VALUES
    (5001, 4001, 'staple', '主食', 'RICH_TEXT', 1, 1, 1, NOW(), NOW(), 0),
    (5002, 4001, 'protein', '蛋白', 'RICH_TEXT', 2, 1, 1, NOW(), NOW(), 0),
    (5003, 4001, 'vegetable', '蔬菜', 'RICH_TEXT', 3, 1, 1, NOW(), NOW(), 0),
    (5004, 4001, 'other', '其他', 'RICH_TEXT', 4, 1, 1, NOW(), NOW(), 0),
    (5005, 4002, 'snack', '加餐', 'RICH_TEXT', 1, 1, 1, NOW(), NOW(), 0),
    (5006, 4003, 'staple', '主食', 'RICH_TEXT', 1, 1, 1, NOW(), NOW(), 0),
    (5007, 4003, 'protein', '蛋白', 'RICH_TEXT', 2, 1, 1, NOW(), NOW(), 0),
    (5008, 4003, 'vegetable', '蔬菜', 'RICH_TEXT', 3, 1, 1, NOW(), NOW(), 0),
    (5009, 4003, 'other', '其他', 'RICH_TEXT', 4, 1, 1, NOW(), NOW(), 0),
    (5010, 4004, 'snack', '加餐', 'RICH_TEXT', 1, 1, 1, NOW(), NOW(), 0),
    (5011, 4005, 'staple', '主食', 'RICH_TEXT', 1, 1, 1, NOW(), NOW(), 0),
    (5012, 4005, 'protein', '蛋白', 'RICH_TEXT', 2, 1, 1, NOW(), NOW(), 0),
    (5013, 4005, 'vegetable', '蔬菜', 'RICH_TEXT', 3, 1, 1, NOW(), NOW(), 0),
    (5014, 4005, 'other', '其他', 'RICH_TEXT', 4, 1, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE update_time = NOW();
