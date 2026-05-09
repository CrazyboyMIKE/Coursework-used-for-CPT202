-- Phase 3A database audit for local/dev/test data only.
-- Expected result for every query before Phase 3B migration: zero rows.
-- Do not run this script against a production database.

-- 1. Negative specialist base fees
-- Checks: specialist profiles whose base_fee is below zero.
-- Why it matters: specialist pricing and booking price calculation assume non-negative fees.
-- Expected before migration: zero rows.
SELECT
    id,
    user_id,
    category_id,
    base_fee
FROM specialist_profiles
WHERE base_fee < 0;

-- 2. Negative booking prices
-- Checks: bookings whose stored price is below zero.
-- Why it matters: reporting revenue and booking history assume non-negative prices.
-- Expected before migration: zero rows.
SELECT
    id,
    customer_id,
    specialist_id,
    slot_id,
    price,
    status
FROM bookings
WHERE price < 0;

-- 3. Invalid slot times
-- Checks: time slots whose end_time is not after start_time.
-- Why it matters: slot availability, booking validation, and calendar display assume a positive time window.
-- Expected before migration: zero rows.
SELECT
    id,
    specialist_id,
    start_time,
    end_time,
    status
FROM time_slots
WHERE end_time <= start_time;

-- 4. Duplicate exact slots
-- Checks: identical slot windows for the same specialist.
-- Why it matters: duplicate slots can confuse booking availability even if overlapping slots are checked in service logic.
-- Expected before migration: zero rows.
SELECT
    specialist_id,
    start_time,
    end_time,
    COUNT(*) AS duplicate_count
FROM time_slots
GROUP BY specialist_id, start_time, end_time
HAVING COUNT(*) > 1;

-- 5. Booking-specialist mismatch
-- Checks: bookings where bookings.specialist_id differs from the specialist that owns bookings.slot_id.
-- Why it matters: the current schema stores both booking.specialist_id and booking.slot_id; service logic assumes they agree.
-- Expected before migration: zero rows.
SELECT
    b.id AS booking_id,
    b.customer_id,
    b.specialist_id AS booking_specialist_id,
    b.slot_id,
    ts.specialist_id AS slot_specialist_id,
    b.status
FROM bookings b
JOIN time_slots ts ON ts.id = b.slot_id
WHERE b.specialist_id <> ts.specialist_id;
