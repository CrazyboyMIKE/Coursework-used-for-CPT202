CREATE TABLE expertise_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(255),
    active BIT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_expertise_categories_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL,
    phone VARCHAR(30),
    role VARCHAR(20) NOT NULL,
    active BIT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_accounts_username UNIQUE (username),
    CONSTRAINT uk_user_accounts_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE specialist_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    level VARCHAR(120) NOT NULL,
    base_fee DECIMAL(10, 2) NOT NULL,
    fee_currency VARCHAR(3),
    status VARCHAR(20) NOT NULL,
    bio VARCHAR(500),
    PRIMARY KEY (id),
    CONSTRAINT uk_specialist_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_specialist_profiles_user FOREIGN KEY (user_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_specialist_profiles_category FOREIGN KEY (category_id) REFERENCES expertise_categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE time_slots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    specialist_id BIGINT NOT NULL,
    start_time DATETIME(6) NOT NULL,
    end_time DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_time_slots_specialist FOREIGN KEY (specialist_id) REFERENCES specialist_profiles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bookings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    specialist_id BIGINT NOT NULL,
    slot_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    topic VARCHAR(120) NOT NULL,
    notes VARCHAR(500),
    price DECIMAL(10, 2) NOT NULL,
    last_action_reason VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_bookings_specialist FOREIGN KEY (specialist_id) REFERENCES specialist_profiles (id),
    CONSTRAINT fk_bookings_slot FOREIGN KEY (slot_id) REFERENCES time_slots (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE session_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    active BIT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    last_used_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_session_tokens_token UNIQUE (token),
    CONSTRAINT fk_session_tokens_user FOREIGN KEY (user_id) REFERENCES user_accounts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    active BIT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_tokens_token UNIQUE (token),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES user_accounts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_specialist_profiles_category ON specialist_profiles (category_id);
CREATE INDEX idx_time_slots_specialist_start ON time_slots (specialist_id, start_time);
CREATE INDEX idx_time_slots_specialist_status_start ON time_slots (specialist_id, status, start_time);
CREATE INDEX idx_bookings_customer_slot_start ON bookings (customer_id, slot_id);
CREATE INDEX idx_bookings_specialist_status ON bookings (specialist_id, status);
CREATE INDEX idx_bookings_status ON bookings (status);
CREATE INDEX idx_session_tokens_user_active ON session_tokens (user_id, active);
CREATE INDEX idx_password_reset_tokens_user_active ON password_reset_tokens (user_id, active);
