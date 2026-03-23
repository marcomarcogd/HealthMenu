CREATE TABLE IF NOT EXISTS customer (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    nickname VARCHAR(64),
    gender VARCHAR(16),
    phone VARCHAR(32),
    exclusive_title VARCHAR(255),
    note TEXT,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS menu_template (
    id BIGINT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    is_default TINYINT NOT NULL DEFAULT 0,
    theme_code VARCHAR(64),
    cover_image_path VARCHAR(255),
    title_rule VARCHAR(255),
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS menu_template_section (
    id BIGINT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    section_type VARCHAR(64) NOT NULL,
    title VARCHAR(128),
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    style_config_json TEXT,
    allow_image TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS menu_template_meal (
    id BIGINT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    meal_code VARCHAR(64) NOT NULL,
    meal_name VARCHAR(64) NOT NULL,
    time_label VARCHAR(64),
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS menu_template_meal_item (
    id BIGINT PRIMARY KEY,
    template_meal_id BIGINT NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(64) NOT NULL,
    content_format VARCHAR(32) NOT NULL DEFAULT 'RICH_TEXT',
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    allow_image TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS customer_menu (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    menu_date DATE NOT NULL,
    week_index INT,
    title VARCHAR(255),
    show_weekly_tip TINYINT NOT NULL DEFAULT 1,
    show_swap_guide TINYINT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    share_token VARCHAR(64),
    theme_code VARCHAR(64),
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS customer_menu_section_content (
    id BIGINT PRIMARY KEY,
    customer_menu_id BIGINT NOT NULL,
    section_type VARCHAR(64) NOT NULL,
    title VARCHAR(128),
    content LONGTEXT,
    style_json TEXT,
    image_path VARCHAR(255),
    ai_image_prompt VARCHAR(255),
    ai_image_task_id VARCHAR(128),
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS customer_menu_meal (
    id BIGINT PRIMARY KEY,
    customer_menu_id BIGINT NOT NULL,
    meal_code VARCHAR(64) NOT NULL,
    meal_name VARCHAR(64) NOT NULL,
    time_label VARCHAR(64),
    meal_time VARCHAR(64),
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS customer_menu_meal_item (
    id BIGINT PRIMARY KEY,
    customer_menu_meal_id BIGINT NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(64) NOT NULL,
    item_value LONGTEXT,
    style_json TEXT,
    image_path VARCHAR(255),
    ai_image_prompt VARCHAR(255),
    ai_image_task_id VARCHAR(128),
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS menu_publish_record (
    id BIGINT PRIMARY KEY,
    customer_menu_id BIGINT NOT NULL,
    export_type VARCHAR(32) NOT NULL,
    file_path VARCHAR(255),
    file_name VARCHAR(255),
    operator_name VARCHAR(64),
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ai_import_record (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT,
    source_type VARCHAR(32) NOT NULL,
    source_text LONGTEXT,
    source_image_path VARCHAR(255),
    parsed_json LONGTEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    provider VARCHAR(32),
    workflow_code VARCHAR(64),
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id BIGINT PRIMARY KEY,
    type_code VARCHAR(64) NOT NULL,
    type_name VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_dict_item (
    id BIGINT PRIMARY KEY,
    dict_type_id BIGINT NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_label VARCHAR(128) NOT NULL,
    item_value VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_system TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

INSERT INTO menu_template (id, name, description, is_default, theme_code, cover_image_path, title_rule, status, create_time, update_time, deleted)
VALUES (1001, '标准模板', '默认模板，含专属标题、每周提示、互换指南与每日餐单', 1, 'standard', NULL, '{{customerName}}专属餐单', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_dict_type (id, type_code, type_name, description, status, create_time, update_time, deleted)
VALUES
    (2001, 'gender', '性别', '客户性别选项', 1, NOW(), NOW(), 0),
    (2002, 'theme_code', '模板主题', '模板和餐单可选主题', 1, NOW(), NOW(), 0),
    (2003, 'meal_code', '餐次编码', '模板设计器中的餐次选项', 1, NOW(), NOW(), 0),
    (2004, 'item_code', '字段编码', '餐次内字段项选项', 1, NOW(), NOW(), 0),
    (2005, 'section_type', '区块类型', '模板区块类型选项', 1, NOW(), NOW(), 0),
    (2006, 'record_status', '记录状态', '餐单状态选项', 1, NOW(), NOW(), 0),
    (2007, 'content_format', '内容格式', '模板字段内容格式', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_dict_item (id, dict_type_id, item_code, item_label, item_value, sort_order, is_system, status, create_time, update_time, deleted)
VALUES
    (3001, 2001, 'male', '男', 'male', 1, 1, 1, NOW(), NOW(), 0),
    (3002, 2001, 'female', '女', 'female', 2, 1, 1, NOW(), NOW(), 0),
    (3003, 2001, 'unknown', '未设置', 'unknown', 99, 1, 1, NOW(), NOW(), 0),
    (3011, 2002, 'standard', '标准主题', 'standard', 1, 1, 1, NOW(), NOW(), 0),
    (3012, 2002, 'warm', '温暖主题', 'warm', 2, 1, 1, NOW(), NOW(), 0),
    (3013, 2002, 'fresh', '清新主题', 'fresh', 3, 1, 1, NOW(), NOW(), 0),
    (3021, 2003, 'breakfast', '早餐', 'breakfast', 1, 1, 1, NOW(), NOW(), 0),
    (3022, 2003, 'lunch', '午餐', 'lunch', 2, 1, 1, NOW(), NOW(), 0),
    (3023, 2003, 'dinner', '晚餐', 'dinner', 3, 1, 1, NOW(), NOW(), 0),
    (3024, 2003, 'snack', '加餐', 'snack', 4, 1, 1, NOW(), NOW(), 0),
    (3031, 2004, 'staple', '主食', 'staple', 1, 1, 1, NOW(), NOW(), 0),
    (3032, 2004, 'protein', '蛋白', 'protein', 2, 1, 1, NOW(), NOW(), 0),
    (3033, 2004, 'vegetable', '蔬菜', 'vegetable', 3, 1, 1, NOW(), NOW(), 0),
    (3034, 2004, 'fruit', '水果', 'fruit', 4, 1, 1, NOW(), NOW(), 0),
    (3035, 2004, 'other', '其他', 'other', 5, 1, 1, NOW(), NOW(), 0),
    (3041, 2005, 'EXCLUSIVE_TITLE', '专属标题', 'EXCLUSIVE_TITLE', 1, 1, 1, NOW(), NOW(), 0),
    (3042, 2005, 'WEEKLY_TIP', '每周提示', 'WEEKLY_TIP', 2, 1, 1, NOW(), NOW(), 0),
    (3043, 2005, 'SWAP_GUIDE', '互换指南', 'SWAP_GUIDE', 3, 1, 1, NOW(), NOW(), 0),
    (3044, 2005, 'DAILY_MENU', '每日餐单', 'DAILY_MENU', 4, 1, 1, NOW(), NOW(), 0),
    (3045, 2005, 'REMARK', '备注', 'REMARK', 5, 1, 1, NOW(), NOW(), 0),
    (3046, 2005, 'IMAGE_BLOCK', '图片区块', 'IMAGE_BLOCK', 6, 1, 1, NOW(), NOW(), 0),
    (3051, 2006, 'DRAFT', '草稿', 'DRAFT', 1, 1, 1, NOW(), NOW(), 0),
    (3052, 2006, 'PUBLISHED', '已发布', 'PUBLISHED', 2, 1, 1, NOW(), NOW(), 0),
    (3061, 2007, 'PLAIN_TEXT', '纯文本', 'PLAIN_TEXT', 1, 1, 1, NOW(), NOW(), 0),
    (3062, 2007, 'RICH_TEXT', '富文本', 'RICH_TEXT', 2, 1, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE update_time = NOW();

