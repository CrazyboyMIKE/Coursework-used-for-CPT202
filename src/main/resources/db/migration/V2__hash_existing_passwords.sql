UPDATE user_accounts
SET password = CONCAT('sha256$CPT202-28$', SHA2(CONCAT('CPT202-28', password), 256))
WHERE password NOT LIKE 'sha256$CPT202-28$%';
