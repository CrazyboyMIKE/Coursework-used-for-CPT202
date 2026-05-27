UPDATE user_accounts
SET phone = NULL
WHERE phone IS NOT NULL AND TRIM(phone) = '';

ALTER TABLE user_accounts
    ADD CONSTRAINT uk_user_accounts_phone UNIQUE (phone);

CREATE TABLE access_audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT,
    request_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    response_status INT NOT NULL,
    reason VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_access_audit_logs_user FOREIGN KEY (user_id) REFERENCES user_accounts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_access_audit_logs_user_created ON access_audit_logs (user_id, created_at);
CREATE INDEX idx_access_audit_logs_status_created ON access_audit_logs (response_status, created_at);
