CREATE TABLE IF NOT EXISTS customer (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    nickname VARCHAR(64),
    gender VARCHAR(16),
    phone VARCHAR(32),
    exclusive_title VARCHAR(255),
    note TEXT,
    status TINYINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
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
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
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
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
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
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
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
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
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
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS customer_menu_section_content (
    id BIGINT PRIMARY KEY,
    customer_menu_id BIGINT NOT NULL,
    section_type VARCHAR(64) NOT NULL,
    title VARCHAR(128),
    content CLOB,
    style_json TEXT,
    image_path VARCHAR(255),
    ai_image_prompt VARCHAR(255),
    ai_image_task_id VARCHAR(128),
    sort_order INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
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
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS customer_menu_meal_item (
    id BIGINT PRIMARY KEY,
    customer_menu_meal_id BIGINT NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(64) NOT NULL,
    item_value CLOB,
    style_json TEXT,
    image_path VARCHAR(255),
    ai_image_prompt VARCHAR(255),
    ai_image_task_id VARCHAR(128),
    sort_order INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS menu_publish_record (
    id BIGINT PRIMARY KEY,
    customer_menu_id BIGINT NOT NULL,
    export_type VARCHAR(32) NOT NULL,
    file_path VARCHAR(255),
    file_name VARCHAR(255),
    operator_name VARCHAR(64),
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ai_import_record (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT,
    source_type VARCHAR(32) NOT NULL,
    source_text CLOB,
    source_image_path VARCHAR(255),
    parsed_json CLOB,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    provider VARCHAR(32),
    workflow_code VARCHAR(64),
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    last_login_at TIMESTAMP,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS sys_user_audit_log (
    id BIGINT PRIMARY KEY,
    target_user_id BIGINT NOT NULL,
    target_username VARCHAR(64) NOT NULL,
    target_display_name VARCHAR(64),
    operator_user_id BIGINT,
    operator_username VARCHAR(64),
    operator_display_name VARCHAR(64),
    action_code VARCHAR(32) NOT NULL,
    action_label VARCHAR(64) NOT NULL,
    detail VARCHAR(500),
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id BIGINT PRIMARY KEY,
    type_code VARCHAR(64) NOT NULL,
    type_name VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    status TINYINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
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
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);
