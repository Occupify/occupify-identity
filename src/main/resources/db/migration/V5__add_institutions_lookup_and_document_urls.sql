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
INSERT INTO institutions (id, name, code, country, city, website, logo_url, is_verified, created_at, updated_at)
VALUES
    -- =========================================================================
    -- NORTHERN VIETNAM (45 Institutions)
    -- =========================================================================
    (gen_random_uuid(), 'Đại học Bách khoa Hà Nội', 'HUST', 'Vietnam', 'Hà Nội', 'https://hust.edu.vn', 'https://ui-avatars.com/api/?name=HUST&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Công nghệ – ĐHQG Hà Nội', 'UET', 'Vietnam', 'Hà Nội', 'https://uet.vnu.edu.vn', 'https://ui-avatars.com/api/?name=UET&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Khoa học Tự nhiên – ĐHQG Hà Nội', 'HUS', 'Vietnam', 'Hà Nội', 'https://hus.vnu.edu.vn', 'https://ui-avatars.com/api/?name=HUS&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Khoa học Xã hội và Nhân văn – ĐHQG Hà Nội', 'USSH-VNU', 'Vietnam', 'Hà Nội', 'https://ussh.vnu.edu.vn', 'https://ui-avatars.com/api/?name=USSH&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Ngoại ngữ – ĐHQG Hà Nội', 'ULIS', 'Vietnam', 'Hà Nội', 'https://ulis.vnu.edu.vn', 'https://ui-avatars.com/api/?name=ULIS&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Kinh tế – ĐHQG Hà Nội', 'UEB', 'Vietnam', 'Hà Nội', 'https://ueb.edu.vn', 'https://ui-avatars.com/api/?name=UEB&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Giáo dục – ĐHQG Hà Nội', 'UED-VNU', 'Vietnam', 'Hà Nội', 'https://education.vnu.edu.vn', 'https://ui-avatars.com/api/?name=UED&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Y Dược – ĐHQG Hà Nội', 'UMP-VNU', 'Vietnam', 'Hà Nội', 'https://ump.vnu.edu.vn', 'https://ui-avatars.com/api/?name=UMP&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Việt Nhật – ĐHQG Hà Nội', 'VJU', 'Vietnam', 'Hà Nội', 'https://vju.ac.vn', 'https://ui-avatars.com/api/?name=VJU&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Luật – ĐHQG Hà Nội', 'VNU-UL', 'Vietnam', 'Hà Nội', 'https://law.vnu.edu.vn', 'https://ui-avatars.com/api/?name=VNUUL&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Kinh tế Quốc dân', 'NEU', 'Vietnam', 'Hà Nội', 'https://neu.edu.vn', 'https://ui-avatars.com/api/?name=NEU&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Ngoại thương', 'FTU', 'Vietnam', 'Hà Nội', 'https://ftu.edu.vn', 'https://ui-avatars.com/api/?name=FTU&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Thương mại', 'TMU', 'Vietnam', 'Hà Nội', 'https://tmu.edu.vn', 'https://ui-avatars.com/api/?name=TMU&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Học viện Ngân hàng', 'BAV', 'Vietnam', 'Hà Nội', 'https://hvnh.edu.vn', 'https://ui-avatars.com/api/?name=BAV&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Học viện Tài chính', 'AOF', 'Vietnam', 'Hà Nội', 'https://hvtc.edu.vn', 'https://ui-avatars.com/api/?name=AOF&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Học viện Công nghệ Bưu chính Viễn thông', 'PTIT', 'Vietnam', 'Hà Nội', 'https://ptit.edu.vn', 'https://ui-avatars.com/api/?name=PTIT&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Xây dựng Hà Nội', 'HUCE', 'Vietnam', 'Hà Nội', 'https://huce.edu.vn', 'https://ui-avatars.com/api/?name=HUCE&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Giao thông Vận tải', 'UTC', 'Vietnam', 'Hà Nội', 'https://utc.edu.vn', 'https://ui-avatars.com/api/?name=UTC&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Thủy lợi', 'TLU', 'Vietnam', 'Hà Nội', 'https://tlu.edu.vn', 'https://ui-avatars.com/api/?name=TLU&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Mỏ – Địa chất', 'HUMG', 'Vietnam', 'Hà Nội', 'https://humg.edu.vn', 'https://ui-avatars.com/api/?name=HUMG&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Công nghiệp Hà Nội', 'HAUI', 'Vietnam', 'Hà Nội', 'https://haui.edu.vn', 'https://ui-avatars.com/api/?name=HAUI&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Hà Nội', 'HANU', 'Vietnam', 'Hà Nội', 'https://hanu.vn', 'https://ui-avatars.com/api/?name=HANU&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Sư phạm Hà Nội', 'HNUE', 'Vietnam', 'Hà Nội', 'https://hnue.edu.vn', 'https://ui-avatars.com/api/?name=HNUE&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Y Hà Nội', 'HMU', 'Vietnam', 'Hà Nội', 'https://hmu.edu.vn', 'https://ui-avatars.com/api/?name=HMU&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Dược Hà Nội', 'HUP', 'Vietnam', 'Hà Nội', 'https://hup.edu.vn', 'https://ui-avatars.com/api/?name=HUP&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Luật Hà Nội', 'HLU', 'Vietnam', 'Hà Nội', 'https://hlu.edu.vn', 'https://ui-avatars.com/api/?name=HLU&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Học viện Ngoại giao', 'DAV', 'Vietnam', 'Hà Nội', 'https://dav.edu.vn', 'https://ui-avatars.com/api/?name=DAV&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Học viện Báo chí và Tuyên truyền', 'AJC', 'Vietnam', 'Hà Nội', 'https://ajc.hcma.vn', 'https://ui-avatars.com/api/?name=AJC&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Học viện Nông nghiệp Việt Nam', 'VNUA', 'Vietnam', 'Hà Nội', 'https://vnua.edu.vn', 'https://ui-avatars.com/api/?name=VNUA&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Lâm nghiệp', 'VNUF', 'Vietnam', 'Hà Nội', 'https://vnuf.edu.vn', 'https://ui-avatars.com/api/?name=VNUF&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Kiến trúc Hà Nội', 'HAU', 'Vietnam', 'Hà Nội', 'https://hau.edu.vn', 'https://ui-avatars.com/api/?name=HAU&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Mỹ thuật Công nghiệp', 'UAD', 'Vietnam', 'Hà Nội', 'https://mythuatcongnghiep.edu.vn', 'https://ui-avatars.com/api/?name=UAD&background=0D47A1&color=fff&size=256&bold=true', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Văn hóa Hà Nội', 'HUC', 'Vietnam', 'Hà Nội', 'https://huc.edu.vn', 'https://ui-avatars.com/api/?name=HUC&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Mở Hà Nội', 'HOU', 'Vietnam', 'Hà Nội', 'https://hou.edu.vn', 'https://ui-avatars.com/api/?name=HOU&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Điện lực', 'EPU', 'Vietnam', 'Hà Nội', 'https://epu.edu.vn', 'https://ui-avatars.com/api/?name=EPU&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Công đoàn', 'TUU', 'Vietnam', 'Hà Nội', 'https://dhcd.edu.vn', 'https://ui-avatars.com/api/?name=TUU&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Lao động – Xã hội', 'ULSA', 'Vietnam', 'Hà Nội', 'https://ulsa.edu.vn', 'https://ui-avatars.com/api/?name=ULSA&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Tài nguyên và Môi trường Hà Nội', 'HUNRE', 'Vietnam', 'Hà Nội', 'https://hunre.edu.vn', 'https://ui-avatars.com/api/?name=HUNRE&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học FPT', 'FPTU', 'Vietnam', 'Hà Nội', 'https://uni.fpt.edu.vn', 'https://ui-avatars.com/api/?name=FPTU&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Đại học Phenikaa', 'PKA', 'Vietnam', 'Hà Nội', 'https://phenikaa-uni.edu.vn', 'https://ui-avatars.com/api/?name=PKA&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học VinUni', 'VINUNI', 'Vietnam', 'Hà Nội', 'https://vinuni.edu.vn', 'https://ui-avatars.com/api/?name=VINUNI&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Thăng Long', 'TLU-HN', 'Vietnam', 'Hà Nội', 'https://thanglong.edu.vn', 'https://ui-avatars.com/api/?name=TLUHN&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Hàng hải Việt Nam', 'VMU', 'Vietnam', 'Hải Phòng', 'https://vimaru.edu.vn', 'https://ui-avatars.com/api/?name=VMU&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Hải Phòng', 'DHHP', 'Vietnam', 'Hải Phòng', 'https://dhhp.edu.vn', 'https://ui-avatars.com/api/?name=DHHP&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Đại học Thái Nguyên', 'TNU', 'Vietnam', 'Thái Nguyên', 'https://tnu.edu.vn', 'https://ui-avatars.com/api/?name=TNU&background=0D47A1&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- =========================================================================
    -- CENTRAL VIETNAM & CENTRAL HIGHLANDS (17 Institutions)
    -- =========================================================================
    (gen_random_uuid(), 'Trường Đại học Bách khoa – Đại học Đà Nẵng', 'DUT', 'Vietnam', 'Đà Nẵng', 'https://dut.udn.vn', 'https://ui-avatars.com/api/?name=DUT&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Kinh tế – Đại học Đà Nẵng', 'DUE', 'Vietnam', 'Đà Nẵng', 'https://due.udn.vn', 'https://ui-avatars.com/api/?name=DUE&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Sư phạm – Đại học Đà Nẵng', 'UED', 'Vietnam', 'Đà Nẵng', 'https://ued.udn.vn', 'https://ui-avatars.com/api/?name=UED&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Ngoại ngữ – Đại học Đà Nẵng', 'UFLS', 'Vietnam', 'Đà Nẵng', 'https://ufl.udn.vn', 'https://ui-avatars.com/api/?name=UFLS&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Sư phạm Kỹ thuật – Đại học Đà Nẵng', 'UTE-UDN', 'Vietnam', 'Đà Nẵng', 'https://ute.udn.vn', 'https://ui-avatars.com/api/?name=UTE&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Công nghệ Thông tin và Truyền thông Việt – Hàn', 'VKU', 'Vietnam', 'Đà Nẵng', 'https://vku.udn.vn', 'https://ui-avatars.com/api/?name=VKU&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Đại học Duy Tân', 'DTU', 'Vietnam', 'Đà Nẵng', 'https://duytan.edu.vn', 'https://ui-avatars.com/api/?name=DTU&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Kiến trúc Đà Nẵng', 'DAU', 'Vietnam', 'Đà Nẵng', 'https://dau.edu.vn', 'https://ui-avatars.com/api/?name=DAU&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Đông Á', 'UDA', 'Vietnam', 'Đà Nẵng', 'https://donga.edu.vn', 'https://ui-avatars.com/api/?name=UDA&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Đại học Huế', 'DHH', 'Vietnam', 'Thừa Thiên Huế', 'https://hueuni.edu.vn', 'https://ui-avatars.com/api/?name=DHH&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Y Dược – Đại học Huế', 'HUMP', 'Vietnam', 'Thừa Thiên Huế', 'https://huump.edu.vn', 'https://ui-avatars.com/api/?name=HUMP&background=1B5E20&color=fff&size=256&bold=true', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Khoa học – Đại học Huế', 'HUSC', 'Vietnam', 'Thừa Thiên Huế', 'https://husc.edu.vn', 'https://ui-avatars.com/api/?name=HUSC&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Kinh tế – Đại học Huế', 'HCE', 'Vietnam', 'Thừa Thiên Huế', 'https://hce.edu.vn', 'https://ui-avatars.com/api/?name=HCE&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Vinh', 'VINHUNI', 'Vietnam', 'Nghệ An', 'https://vinhuni.edu.vn', 'https://ui-avatars.com/api/?name=VINHUNI&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Hồng Đức', 'HDU', 'Vietnam', 'Thanh Hóa', 'https://hdu.edu.vn', 'https://ui-avatars.com/api/?name=HDU&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Quy Nhơn', 'QNU', 'Vietnam', 'Bình Định', 'https://qnu.edu.vn', 'https://ui-avatars.com/api/?name=QNU&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Nha Trang', 'NTU', 'Vietnam', 'Khánh Hòa', 'https://ntu.edu.vn', 'https://ui-avatars.com/api/?name=NTU&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Đà Lạt', 'DLU', 'Vietnam', 'Lâm Đồng', 'https://dlu.edu.vn', 'https://ui-avatars.com/api/?name=DLU&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Tây Nguyên', 'TTN', 'Vietnam', 'Đắk Lắk', 'https://ttn.edu.vn', 'https://ui-avatars.com/api/?name=TTN&background=1B5E20&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- =========================================================================
    -- SOUTHERN VIETNAM (36 Institutions)
    -- =========================================================================
    (gen_random_uuid(), 'Trường Đại học Bách khoa – ĐHQG TP.HCM', 'HCMUT', 'Vietnam', 'TP. Hồ Chí Minh', 'https://hcmut.edu.vn', 'https://ui-avatars.com/api/?name=HCMUT&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Khoa học Tự nhiên – ĐHQG TP.HCM', 'HCMUS', 'Vietnam', 'TP. Hồ Chí Minh', 'https://hcmus.edu.vn', 'https://ui-avatars.com/api/?name=HCMUS&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Công nghệ Thông tin – ĐHQG TP.HCM', 'UIT', 'Vietnam', 'TP. Hồ Chí Minh', 'https://uit.edu.vn', 'https://ui-avatars.com/api/?name=UIT&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Khoa học Xã hội và Nhân văn – ĐHQG TP.HCM', 'HCMUSSH', 'Vietnam', 'TP. Hồ Chí Minh', 'https://hcmussh.edu.vn', 'https://ui-avatars.com/api/?name=HCMUSSH&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Quốc tế – ĐHQG TP.HCM', 'HCMIU', 'Vietnam', 'TP. Hồ Chí Minh', 'https://hcmiu.edu.vn', 'https://ui-avatars.com/api/?name=HCMIU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Kinh tế – Luật – ĐHQG TP.HCM', 'UEL', 'Vietnam', 'TP. Hồ Chí Minh', 'https://uel.edu.vn', 'https://ui-avatars.com/api/?name=UEL&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Khoa học Sức khỏe – ĐHQG TP.HCM', 'UHS-VNUHCM', 'Vietnam', 'TP. Hồ Chí Minh', 'https://uhsvnu.edu.vn', 'https://ui-avatars.com/api/?name=UHS&background=B71C1C&color=fff&size=256&bold=true', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học An Giang – ĐHQG TP.HCM', 'AGU', 'Vietnam', 'An Giang', 'https://agu.edu.vn', 'https://ui-avatars.com/api/?name=AGU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Đại học Kinh tế TP. Hồ Chí Minh', 'UEH', 'Vietnam', 'TP. Hồ Chí Minh', 'https://ueh.edu.vn', 'https://ui-avatars.com/api/?name=UEH&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Sư phạm Kỹ thuật TP. Hồ Chí Minh', 'HCMUTE', 'Vietnam', 'TP. Hồ Chí Minh', 'https://hcmute.edu.vn', 'https://ui-avatars.com/api/?name=HCMUTE&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Tôn Đức Thắng', 'TDTU', 'Vietnam', 'TP. Hồ Chí Minh', 'https://tdtu.edu.vn', 'https://ui-avatars.com/api/?name=TDTU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Công nghiệp TP. Hồ Chí Minh', 'IUH', 'Vietnam', 'TP. Hồ Chí Minh', 'https://iuh.edu.vn', 'https://ui-avatars.com/api/?name=IUH&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Công Thương TP. Hồ Chí Minh', 'HUIT', 'Vietnam', 'TP. Hồ Chí Minh', 'https://huit.edu.vn', 'https://ui-avatars.com/api/?name=HUIT&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Sài Gòn', 'SGU', 'Vietnam', 'TP. Hồ Chí Minh', 'https://sgu.edu.vn', 'https://ui-avatars.com/api/?name=SGU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Sư phạm TP. Hồ Chí Minh', 'HCMUE', 'Vietnam', 'TP. Hồ Chí Minh', 'https://hcmue.edu.vn', 'https://ui-avatars.com/api/?name=HCMUE&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Y Dược TP. Hồ Chí Minh', 'UMP', 'Vietnam', 'TP. Hồ Chí Minh', 'https://ump.edu.vn', 'https://ui-avatars.com/api/?name=UMP&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Y khoa Phạm Ngọc Thạch', 'PNTU', 'Vietnam', 'TP. Hồ Chí Minh', 'https://pnt.edu.vn', 'https://ui-avatars.com/api/?name=PNTU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Luật TP. Hồ Chí Minh', 'HCMULAW', 'Vietnam', 'TP. Hồ Chí Minh', 'https://hcmulaw.edu.vn', 'https://ui-avatars.com/api/?name=HCMULAW&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Ngân hàng TP. Hồ Chí Minh', 'HUB', 'Vietnam', 'TP. Hồ Chí Minh', 'https://hub.edu.vn', 'https://ui-avatars.com/api/?name=HUB&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Tài chính – Marketing', 'UFM', 'Vietnam', 'TP. Hồ Chí Minh', 'https://ufm.edu.vn', 'https://ui-avatars.com/api/?name=UFM&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Mở TP. Hồ Chí Minh', 'HCMCOU', 'Vietnam', 'TP. Hồ Chí Minh', 'https://ou.edu.vn', 'https://ui-avatars.com/api/?name=HCMCOU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Giao thông Vận tải TP. Hồ Chí Minh', 'UTH', 'Vietnam', 'TP. Hồ Chí Minh', 'https://ut.edu.vn', 'https://ui-avatars.com/api/?name=UTH&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Nông Lâm TP. Hồ Chí Minh', 'NLU', 'Vietnam', 'TP. Hồ Chí Minh', 'https://hcmuaf.edu.vn', 'https://ui-avatars.com/api/?name=NLU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Kiến trúc TP. Hồ Chí Minh', 'UAH', 'Vietnam', 'TP. Hồ Chí Minh', 'https://uah.edu.vn', 'https://ui-avatars.com/api/?name=UAH&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Tài nguyên và Môi trường TP. Hồ Chí Minh', 'HCMUNRE', 'Vietnam', 'TP. Hồ Chí Minh', 'https://hcmunre.edu.vn', 'https://ui-avatars.com/api/?name=HCMUNRE&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Văn Lang', 'VLU', 'Vietnam', 'TP. Hồ Chí Minh', 'https://vlu.edu.vn', 'https://ui-avatars.com/api/?name=VLU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Công nghệ TP. Hồ Chí Minh', 'HUTECH', 'Vietnam', 'TP. Hồ Chí Minh', 'https://hutech.edu.vn', 'https://ui-avatars.com/api/?name=HUTECH&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Hoa Sen', 'HSU', 'Vietnam', 'TP. Hồ Chí Minh', 'https://hoasen.edu.vn', 'https://ui-avatars.com/api/?name=HSU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Kinh tế – Tài chính TP. Hồ Chí Minh', 'UEF', 'Vietnam', 'TP. Hồ Chí Minh', 'https://uef.edu.vn', 'https://ui-avatars.com/api/?name=UEF&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Nguyễn Tất Thành', 'NTTU', 'Vietnam', 'TP. Hồ Chí Minh', 'https://ntt.edu.vn', 'https://ui-avatars.com/api/?name=NTTU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Quốc tế Hồng Bàng', 'HIU', 'Vietnam', 'TP. Hồ Chí Minh', 'https://hiu.vn', 'https://ui-avatars.com/api/?name=HIU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Gia Định', 'GDU', 'Vietnam', 'TP. Hồ Chí Minh', 'https://giadinh.edu.vn', 'https://ui-avatars.com/api/?name=GDU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Văn Hiến', 'VHU', 'Vietnam', 'TP. Hồ Chí Minh', 'https://vhu.edu.vn', 'https://ui-avatars.com/api/?name=VHU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Ngoại ngữ – Tin học TP. Hồ Chí Minh', 'HUFLIT', 'Vietnam', 'TP. Hồ Chí Minh', 'https://huflit.edu.vn', 'https://ui-avatars.com/api/?name=HUFLIT&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Quốc tế Sài Gòn', 'SIU', 'Vietnam', 'TP. Hồ Chí Minh', 'https://siu.edu.vn', 'https://ui-avatars.com/api/?name=SIU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Đại học RMIT Việt Nam', 'RMIT', 'Vietnam', 'TP. Hồ Chí Minh', 'https://rmit.edu.vn', 'https://ui-avatars.com/api/?name=RMIT&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Fulbright Việt Nam', 'FUV', 'Vietnam', 'TP. Hồ Chí Minh', 'https://fulbright.edu.vn', 'https://ui-avatars.com/api/?name=FUV&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Việt Đức', 'VGU', 'Vietnam', 'Bình Dương', 'https://vgu.edu.vn', 'https://ui-avatars.com/api/?name=VGU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Thủ Dầu Một', 'TDMU', 'Vietnam', 'Bình Dương', 'https://tdmu.edu.vn', 'https://ui-avatars.com/api/?name=TDMU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Quốc tế Miền Đông', 'EIU', 'Vietnam', 'Bình Dương', 'https://eiu.edu.vn', 'https://ui-avatars.com/api/?name=EIU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Lạc Hồng', 'LHU', 'Vietnam', 'Đồng Nai', 'https://lhu.edu.vn', 'https://ui-avatars.com/api/?name=LHU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Đại học Cần Thơ', 'CTU', 'Vietnam', 'Cần Thơ', 'https://ctu.edu.vn', 'https://ui-avatars.com/api/?name=CTU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Y Dược Cần Thơ', 'CTUMP', 'Vietnam', 'Cần Thơ', 'https://ctump.edu.vn', 'https://ui-avatars.com/api/?name=CTUMP&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Nam Cần Thơ', 'DNC', 'Vietnam', 'Cần Thơ', 'https://nctu.edu.vn', 'https://ui-avatars.com/api/?name=DNC&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Trà Vinh', 'TVU', 'Vietnam', 'Trà Vinh', 'https://tvu.edu.vn', 'https://ui-avatars.com/api/?name=TVU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Trường Đại học Đồng Tháp', 'DThU', 'Vietnam', 'Đồng Tháp', 'https://dthu.edu.vn', 'https://ui-avatars.com/api/?name=DThU&background=B71C1C&color=fff&size=256&bold=true', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)

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
