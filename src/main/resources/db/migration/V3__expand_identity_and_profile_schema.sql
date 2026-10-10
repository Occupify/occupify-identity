-- =========================================================================
-- V3: Expand Identity & Profile Schema (Standardized with Profile Survey)
-- Experiences, Educations, Qualifications, Skills, SkillsUsers, Contracts, Reviews
-- =========================================================================

-- 1. NÂNG CẤP BẢNG USERS (Expand existing users table)
DO $$ 
BEGIN 
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'password') THEN 
        ALTER TABLE users RENAME COLUMN password TO password_hash; 
    END IF; 
END $$;

ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS headline VARCHAR(255),
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS bio TEXT,
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(30),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

-- Đồng bộ dữ liệu cũ: chuyển role 'USER' sang 'EMPLOYEE'
UPDATE users SET role = 'EMPLOYEE' WHERE role = 'USER';

-- Thêm ràng buộc Check Constraint cho role (EMPLOYEE, EMPLOYER, ADMIN)
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role 
    CHECK (role IN ('EMPLOYEE', 'EMPLOYER', 'ADMIN'));

-- Thêm ràng buộc Check Constraint cho status (ACTIVE, INACTIVE, BANNED, DELETED)
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_status;
ALTER TABLE users ADD CONSTRAINT chk_users_status 
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'BANNED', 'DELETED'));

CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users(deleted_at);

-- 2. BẢNG EXPERIENCES (Kinh nghiệm làm việc)
CREATE TABLE IF NOT EXISTS experiences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    organization VARCHAR(200) NOT NULL,
    employment_type VARCHAR(50),
    location VARCHAR(150),
    location_type VARCHAR(30),
    start_date DATE NOT NULL,
    end_date DATE,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_experiences_user_id ON experiences(user_id);

-- 3. BẢNG EDUCATIONS (Học vấn)
CREATE TABLE IF NOT EXISTS educations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    institution VARCHAR(200) NOT NULL,
    field_of_study VARCHAR(150) NOT NULL,
    degree VARCHAR(150),
    grade VARCHAR(50),
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_educations_user_id ON educations(user_id);

-- 4. BẢNG QUALIFICATIONS (Chứng chỉ / Bằng cấp)
CREATE TABLE IF NOT EXISTS qualifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    organization VARCHAR(200) NOT NULL,
    issue_date DATE NOT NULL,
    expiration_date DATE,
    credential_id VARCHAR(100),
    credential_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_qualifications_user_id ON qualifications(user_id);

-- 5. BẢNG SKILLS (Kỹ năng)
CREATE TABLE IF NOT EXISTS skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_skills_name ON skills(name);
CREATE INDEX IF NOT EXISTS idx_skills_category ON skills(category);

-- 6. BẢNG TRUNG GIAN SKILLS_USERS (Quan hệ N-N chuẩn hóa với Composite PK & chi tiết trình độ)
CREATE TABLE IF NOT EXISTS skills_users (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    is_top_skill BOOLEAN NOT NULL DEFAULT FALSE,
    proficiency_level VARCHAR(30) DEFAULT 'INTERMEDIATE',
    years_of_experience INTEGER,
    PRIMARY KEY (user_id, skill_id)
);
CREATE INDEX IF NOT EXISTS idx_skills_users_skill_id ON skills_users(skill_id);

-- 7. BẢNG CONTRACTS (Hợp đồng)
CREATE TABLE IF NOT EXISTS contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_role_id UUID NOT NULL, -- Tham chiếu liên microservice (Không tạo FK DB)
    employer_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    employee_id UUID REFERENCES users(id) ON DELETE RESTRICT,
    source_type VARCHAR(50) NOT NULL,
    source_id UUID NOT NULL,
    agreed_rate NUMERIC(15, 2) NOT NULL,
    salary_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_contracts_project_role_id ON contracts(project_role_id);
CREATE INDEX IF NOT EXISTS idx_contracts_employer_id ON contracts(employer_id);
CREATE INDEX IF NOT EXISTS idx_contracts_employee_id ON contracts(employee_id);
CREATE INDEX IF NOT EXISTS idx_contracts_status ON contracts(status);

-- 8. BẢNG REVIEWS (Đánh giá)
CREATE TABLE IF NOT EXISTS reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    reviewer_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    reviewee_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_reviews_contract_id ON reviews(contract_id);
CREATE INDEX IF NOT EXISTS idx_reviews_reviewer_id ON reviews(reviewer_user_id);
CREATE INDEX IF NOT EXISTS idx_reviews_reviewee_id ON reviews(reviewee_user_id);
