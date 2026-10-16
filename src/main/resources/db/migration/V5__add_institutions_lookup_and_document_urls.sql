-- =========================================================================
-- V5: Add Institutions Lookup Table & Document Proof URLs
-- Provides standardized institutions catalog with forced foreign key
-- and inline proof document URLs for educations and qualifications
-- =========================================================================

-- 1. BẢNG DANH MỤC TRƯỜNG HỌC (INSTITUTIONS)
CREATE TABLE IF NOT EXISTS institutions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL UNIQUE,
    code VARCHAR(50),
    country VARCHAR(100) DEFAULT 'Vietnam',
    city VARCHAR(100),
    website VARCHAR(255),
    logo_url VARCHAR(500),
    is_verified BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_institutions_name ON institutions(name);

-- 2. SEED DANH MỤC TRƯỜNG ĐẠI HỌC (CHỦ YẾU VIỆT NAM & CÁC TRƯỜNG QUỐC TẾ PHỔ BIẾN)
INSERT INTO institutions (id, name, code, country, city, website)
VALUES
    -- Các trường Đại học tại Việt Nam (Phía Bắc & Phía Nam)
    ('66666666-6666-6666-6666-666666660001', 'Hanoi University of Science and Technology (HUST)', 'HUST', 'Vietnam', 'Hanoi', 'https://hust.edu.vn'),
    ('66666666-6666-6666-6666-666666660002', 'RMIT University Vietnam', 'RMIT', 'Vietnam', 'Ho Chi Minh City', 'https://rmit.edu.vn'),
    ('66666666-6666-6666-6666-666666660003', 'VNU University of Engineering and Technology (UET)', 'UET', 'Vietnam', 'Hanoi', 'https://uet.vnu.edu.vn'),
    ('66666666-6666-6666-6666-666666660004', 'VNU-HCM University of Technology (HCMUT)', 'HCMUT', 'Vietnam', 'Ho Chi Minh City', 'https://hcmut.edu.vn'),
    ('66666666-6666-6666-6666-666666660005', 'VNU-HCM University of Information Technology (UIT)', 'UIT', 'Vietnam', 'Ho Chi Minh City', 'https://uit.edu.vn'),
    ('66666666-6666-6666-6666-666666660006', 'VNU-HCM University of Science (HCMUS)', 'HCMUS', 'Vietnam', 'Ho Chi Minh City', 'https://hcmus.edu.vn'),
    ('66666666-6666-6666-6666-666666660007', 'FPT University', 'FPTU', 'Vietnam', 'Hanoi', 'https://fpt.edu.vn'),
    ('66666666-6666-6666-6666-666666660008', 'National Economics University (NEU)', 'NEU', 'Vietnam', 'Hanoi', 'https://neu.edu.vn'),
    ('66666666-6666-6666-6666-666666660009', 'Foreign Trade University (FTU)', 'FTU', 'Vietnam', 'Hanoi', 'https://ftu.edu.vn'),
    ('66666666-6666-6666-6666-666666660010', 'Posts and Telecommunications Institute of Technology (PTIT)', 'PTIT', 'Vietnam', 'Hanoi', 'https://ptit.edu.vn'),
    ('66666666-6666-6666-6666-666666660011', 'University of Economics Ho Chi Minh City (UEH)', 'UEH', 'Vietnam', 'Ho Chi Minh City', 'https://ueh.edu.vn'),
    ('66666666-6666-6666-6666-666666660012', 'HCMUTE - Ho Chi Minh City University of Technology and Education', 'HCMUTE', 'Vietnam', 'Ho Chi Minh City', 'https://hcmute.edu.vn'),
    ('66666666-6666-6666-6666-666666660013', 'Can Tho University (CTU)', 'CTU', 'Vietnam', 'Can Tho', 'https://ctu.edu.vn'),
    ('66666666-6666-6666-6666-666666660014', 'Hanoi Medical University (HMU)', 'HMU', 'Vietnam', 'Hanoi', 'https://hmu.edu.vn'),
    ('66666666-6666-6666-6666-666666660015', 'Fulbright University Vietnam', 'FUV', 'Vietnam', 'Ho Chi Minh City', 'https://fulbright.edu.vn'),
    ('66666666-6666-6666-6666-666666660016', 'British University Vietnam (BUV)', 'BUV', 'Vietnam', 'Hung Yen', 'https://buv.edu.vn'),
    -- Một số trường Quốc tế phổ biến
    ('66666666-6666-6666-6666-666666660017', 'Massachusetts Institute of Technology (MIT)', 'MIT', 'United States', 'Cambridge', 'https://mit.edu'),
    ('66666666-6666-6666-6666-666666660018', 'Stanford University', 'STANFORD', 'United States', 'Stanford', 'https://stanford.edu'),
    ('66666666-6666-6666-6666-666666660019', 'Harvard University', 'HARVARD', 'United States', 'Cambridge', 'https://harvard.edu'),
    ('66666666-6666-6666-6666-666666660020', 'National University of Singapore (NUS)', 'NUS', 'Singapore', 'Singapore', 'https://nus.edu.sg'),
    ('66666666-6666-6666-6666-666666660021', 'University of Oxford', 'OXFORD', 'United Kingdom', 'Oxford', 'https://ox.ac.uk'),
    ('66666666-6666-6666-6666-666666660022', 'University of Cambridge', 'CAMBRIDGE', 'United Kingdom', 'Cambridge', 'https://cam.ac.uk')
ON CONFLICT (name) DO NOTHING;

-- 3. MIGRATION & RÀNG BUỘC CHO BẢNG EDUCATIONS
ALTER TABLE educations
    ADD COLUMN IF NOT EXISTS institution_id UUID,
    ADD COLUMN IF NOT EXISTS document_url VARCHAR(500);

-- Tự động chuyển đổi dữ liệu text hiện có (nếu có trường chưa trong catalog thì thêm vào)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'educations' AND column_name = 'institution') THEN
        INSERT INTO institutions (name, country)
        SELECT DISTINCT institution, 'Vietnam'
        FROM educations
        WHERE institution IS NOT NULL AND institution NOT IN (SELECT name FROM institutions)
        ON CONFLICT (name) DO NOTHING;

        UPDATE educations e
        SET institution_id = i.id
        FROM institutions i
        WHERE e.institution = i.name AND e.institution_id IS NULL;

        -- Xóa cột text cũ institution vì hệ thống đã force chọn qua lookup table
        ALTER TABLE educations DROP COLUMN institution;
    END IF;
END $$;

-- Áp đặt ràng buộc NOT NULL và Foreign Key cho institution_id
ALTER TABLE educations
    ALTER COLUMN institution_id SET NOT NULL;

ALTER TABLE educations DROP CONSTRAINT IF EXISTS fk_educations_institution;
ALTER TABLE educations
    ADD CONSTRAINT fk_educations_institution
    FOREIGN KEY (institution_id) REFERENCES institutions(id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_educations_institution_id ON educations(institution_id);

-- 4. BỔ SUNG DOCUMENT_URL CHO BẢNG QUALIFICATIONS
ALTER TABLE qualifications
    ADD COLUMN IF NOT EXISTS document_url VARCHAR(500);
