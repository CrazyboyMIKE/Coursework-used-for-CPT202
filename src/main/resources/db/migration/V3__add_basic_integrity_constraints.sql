ALTER TABLE specialist_profiles
    ADD CONSTRAINT chk_specialist_profiles_base_fee_non_negative CHECK (base_fee >= 0);

ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_price_non_negative CHECK (price >= 0);

ALTER TABLE time_slots
    ADD CONSTRAINT chk_time_slots_end_after_start CHECK (end_time > start_time);
