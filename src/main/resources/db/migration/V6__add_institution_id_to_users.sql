-- =========================================================================
-- V6: Add Institution FK to Users table
-- Allows associating a registered user with their educational institution
-- =========================================================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS institution_id UUID;

ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_users_institution;
ALTER TABLE users
    ADD CONSTRAINT fk_users_institution
    FOREIGN KEY (institution_id) REFERENCES institutions(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_users_institution_id ON users(institution_id);
