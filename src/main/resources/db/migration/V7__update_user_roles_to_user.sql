-- =========================================================================
-- V7: Update user roles to USER and tighten role check constraint
-- Migrates legacy EMPLOYEE / EMPLOYER roles to USER
-- =========================================================================

-- 1. Drop existing role check constraint first so updates to 'USER' are allowed
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;

-- 2. Migrate legacy role values to 'USER'
UPDATE users 
SET role = 'USER' 
WHERE role IN ('EMPLOYEE', 'EMPLOYER') OR role IS NULL;

-- 3. Re-add check constraint with 'USER' and 'ADMIN'
ALTER TABLE users ADD CONSTRAINT chk_users_role 
    CHECK (role IN ('USER', 'ADMIN'));
