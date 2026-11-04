CREATE TABLE province (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) COLLATE utf8mb4_0900_as_ci NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uk_province_name (name)
) ENGINE=InnoDB;
CREATE TABLE branch (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    province_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_branch_province FOREIGN KEY (province_id) REFERENCES province(id),
    INDEX idx_branch_province_visibility (province_id, active, deleted, id)
) ENGINE=InnoDB;
CREATE TABLE store (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    branch_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_store_branch FOREIGN KEY (branch_id) REFERENCES branch(id),
    INDEX idx_store_branch_visibility (branch_id, deleted, active, id)
) ENGINE=InnoDB;
CREATE TABLE whitelist_store (
    store_id BIGINT NOT NULL PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_whitelist_store FOREIGN KEY (store_id) REFERENCES store(id)
) ENGINE=InnoDB;
CREATE TABLE app_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_app_user_username (username)
) ENGINE=InnoDB;
CREATE TABLE change_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    actor VARCHAR(100) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    before_json TEXT,
    after_json TEXT,
    INDEX idx_change_entity (entity_type, entity_id, occurred_at)
) ENGINE=InnoDB;
