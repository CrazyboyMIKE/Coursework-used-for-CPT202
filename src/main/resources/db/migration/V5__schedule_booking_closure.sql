ALTER TABLE bookings
    ADD COLUMN unit_price DECIMAL(10, 2),
    ADD COLUMN pricing_multiplier DECIMAL(5, 2),
    ADD COLUMN cancelled_at DATETIME(6);

UPDATE bookings
SET unit_price = price,
    pricing_multiplier = 1.00
WHERE unit_price IS NULL
   OR pricing_multiplier IS NULL;

ALTER TABLE bookings
    MODIFY COLUMN unit_price DECIMAL(10, 2) NOT NULL,
    MODIFY COLUMN pricing_multiplier DECIMAL(5, 2) NOT NULL;

CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    booking_id BIGINT,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(120) NOT NULL,
    message VARCHAR(500) NOT NULL,
    is_read BIT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_notifications_booking FOREIGN KEY (booking_id) REFERENCES bookings (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notifications_recipient_created ON notifications (recipient_id, created_at);
CREATE INDEX idx_notifications_booking_type ON notifications (booking_id, type);

CREATE TABLE booking_evaluations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_booking_evaluations_booking UNIQUE (booking_id),
    CONSTRAINT fk_booking_evaluations_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT chk_booking_evaluations_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
