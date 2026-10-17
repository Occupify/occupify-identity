-- =========================================================================
-- V4: Seed Profile Demo Data & Skills Catalog
-- Provides rich test data for Users, Experiences, Educations, Qualifications,
-- Skills Catalog, and User-Skill associations.
-- Default Password for all accounts: password123
-- =========================================================================

-- 1. SEED SYSTEM SKILLS CATALOG (23 standard skills across categories)
INSERT INTO skills (id, name, category)
VALUES
    ('55555555-5555-5555-5555-555555550001', 'Java', 'Backend'),
    ('55555555-5555-5555-5555-555555550002', 'Spring Boot', 'Backend'),
    ('55555555-5555-5555-5555-555555550003', 'Python', 'Backend'),
    ('55555555-5555-5555-5555-555555550004', 'Node.js', 'Backend'),
    ('55555555-5555-5555-5555-555555550005', 'Go', 'Backend'),
    ('55555555-5555-5555-5555-555555550006', 'React', 'Frontend'),
    ('55555555-5555-5555-5555-555555550007', 'TypeScript', 'Frontend'),
    ('55555555-5555-5555-5555-555555550008', 'Next.js', 'Frontend'),
    ('55555555-5555-5555-5555-555555550009', 'Vue.js', 'Frontend'),
    ('55555555-5555-5555-5555-555555550010', 'Tailwind CSS', 'Frontend'),
    ('55555555-5555-5555-5555-555555550011', 'PostgreSQL', 'Database'),
    ('55555555-5555-5555-5555-555555550012', 'Redis', 'Database'),
    ('55555555-5555-5555-5555-555555550013', 'MongoDB', 'Database'),
    ('55555555-5555-5555-5555-555555550014', 'Docker', 'DevOps & Cloud'),
    ('55555555-5555-5555-5555-555555550015', 'Kubernetes', 'DevOps & Cloud'),
    ('55555555-5555-5555-5555-555555550016', 'AWS', 'DevOps & Cloud'),
    ('55555555-5555-5555-5555-555555550017', 'CI/CD Pipelines', 'DevOps & Cloud'),
    ('55555555-5555-5555-5555-555555550018', 'Figma', 'UI/UX Design'),
    ('55555555-5555-5555-5555-555555550019', 'UI/UX Design', 'UI/UX Design'),
    ('55555555-5555-5555-5555-555555550020', 'Wireframing & Prototyping', 'UI/UX Design'),
    ('55555555-5555-5555-5555-555555550021', 'RabbitMQ', 'Architecture & Messaging'),
    ('55555555-5555-5555-5555-555555550022', 'Apache Kafka', 'Architecture & Messaging'),
    ('55555555-5555-5555-5555-555555550023', 'Microservices Architecture', 'Architecture & Messaging')
ON CONFLICT (id) DO UPDATE
    SET name = EXCLUDED.name,
        category = EXCLUDED.category;

-- 2. SEED DEMO USERS (Password: password123)
-- BCrypt hash: $2a$10$gOtSD9sxE43pXEdR0W.UaOJUZIqAiIgI7f01BudsAIFsC6AJfnasq
INSERT INTO users (id, email, password_hash, full_name, headline, avatar_url, bio, phone_number, role, status)
VALUES
    (
        '11111111-1111-1111-1111-111111111111',
        'freelancer@occupify.work',
        '$2a$10$gOtSD9sxE43pXEdR0W.UaOJUZIqAiIgI7f01BudsAIFsC6AJfnasq',
        'Nguyen Van An',
        'Senior Full-Stack Engineer | Java Spring Boot & React Specialist',
        'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500',
        'Passionate Senior Full-Stack Engineer with over 6 years of experience building high-scale distributed microservices, real-time message streaming pipelines, and modern web applications. Strong advocate for Clean Architecture, TDD, and Domain-Driven Design.',
        '+84901234567',
        'EMPLOYEE',
        'ACTIVE'
    ),
    (
        '11111111-1111-1111-1111-111111111112',
        'designer@occupify.work',
        '$2a$10$gOtSD9sxE43pXEdR0W.UaOJUZIqAiIgI7f01BudsAIFsC6AJfnasq',
        'Tran Thi Mai',
        'Lead Product Designer & Design System Architect',
        'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=500',
        'Crafting pixel-perfect, accessible, and high-conversion digital experiences for enterprise SaaS platforms and consumer apps across Southeast Asia.',
        '+84987654321',
        'EMPLOYEE',
        'ACTIVE'
    ),
    (
        '11111111-1111-1111-1111-111111111113',
        'employer@occupify.work',
        '$2a$10$gOtSD9sxE43pXEdR0W.UaOJUZIqAiIgI7f01BudsAIFsC6AJfnasq',
        'Le Hoang Nam',
        'Technical Talent Acquisition Lead @ Occupify Ventures',
        'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500',
        'Connecting visionary tech startups with elite software engineers, architects, and digital product designers.',
        '+84933445566',
        'EMPLOYER',
        'ACTIVE'
    )
ON CONFLICT (id) DO UPDATE
    SET full_name = EXCLUDED.full_name,
        headline = EXCLUDED.headline,
        avatar_url = EXCLUDED.avatar_url,
        bio = EXCLUDED.bio,
        phone_number = EXCLUDED.phone_number,
        role = EXCLUDED.role,
        status = EXCLUDED.status;

-- Đồng thời cập nhật thông tin profile cho user@gmail.com từ V2 nếu có
UPDATE users 
SET full_name = 'Demo User',
    headline = 'Full Stack Developer',
    avatar_url = 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=500',
    bio = 'Default demo user for testing authentication and profile endpoints.',
    phone_number = '+84911223344',
    role = 'EMPLOYEE'
WHERE email = 'user@gmail.com';

-- 3. SEED EXPERIENCES FOR NGUYEN VAN AN (Candidate 1)
INSERT INTO experiences (id, user_id, title, organization, employment_type, location, location_type, start_date, end_date, is_current, description)
VALUES
    (
        '22222222-2222-2222-2222-222222222201',
        '11111111-1111-1111-1111-111111111111',
        'Senior Backend Engineer',
        'VNG Corporation',
        'FULL_TIME',
        'Ho Chi Minh City, Vietnam',
        'HYBRID',
        '2022-03-01',
        NULL,
        TRUE,
        'Architected high-throughput payment processing microservices handling 10,000+ TPS. Migrated legacy monolith services to Spring Boot 3 on Kubernetes with Kafka event streaming and Redis caching.'
    ),
    (
        '22222222-2222-2222-2222-222222222202',
        '11111111-1111-1111-1111-111111111111',
        'Full Stack Developer',
        'FPT Software',
        'FULL_TIME',
        'Hanoi, Vietnam',
        'ON_SITE',
        '2019-07-01',
        '2022-02-28',
        FALSE,
        'Developed enterprise banking portals using Spring Boot, React, and PostgreSQL. Reduced database query latency by 45% through composite indexes, query optimization, and connection pooling tuning.'
    ),
    (
        '22222222-2222-2222-2222-222222222203',
        '11111111-1111-1111-1111-111111111112',
        'Lead Product Designer',
        'MoMo (M-Service)',
        'FULL_TIME',
        'Ho Chi Minh City, Vietnam',
        'REMOTE',
        '2021-06-01',
        NULL,
        TRUE,
        'Led end-to-end design for fintech core payment checkout flows. Built unified multi-brand design tokens in Figma reducing developer handoff time by 30%.'
    )
ON CONFLICT (id) DO UPDATE
    SET title = EXCLUDED.title,
        organization = EXCLUDED.organization,
        employment_type = EXCLUDED.employment_type,
        location = EXCLUDED.location,
        location_type = EXCLUDED.location_type,
        start_date = EXCLUDED.start_date,
        end_date = EXCLUDED.end_date,
        is_current = EXCLUDED.is_current,
        description = EXCLUDED.description;

-- 4. SEED EDUCATIONS
INSERT INTO educations (id, user_id, institution, field_of_study, degree, grade, description, start_date, end_date)
VALUES
    (
        '33333333-3333-3333-3333-333333333301',
        '11111111-1111-1111-1111-111111111111',
        'Hanoi University of Science and Technology (HUST)',
        'Computer Science',
        'Bachelor of Engineering',
        '3.65 / 4.0',
        'Graduated with Honors. Specialized in Distributed Computing, Operating Systems, and Advanced Algorithms.',
        '2015-09-01',
        '2019-06-30'
    ),
    (
        '33333333-3333-3333-3333-333333333302',
        '11111111-1111-1111-1111-111111111112',
        'RMIT University Vietnam',
        'Digital Media Design',
        'Bachelor of Design',
        'High Distinction',
        'Specialized in Interaction Design, User Experience Research, and Enterprise Design Systems.',
        '2017-10-01',
        '2021-04-30'
    )
ON CONFLICT (id) DO UPDATE
    SET institution = EXCLUDED.institution,
        field_of_study = EXCLUDED.field_of_study,
        degree = EXCLUDED.degree,
        grade = EXCLUDED.grade,
        description = EXCLUDED.description,
        start_date = EXCLUDED.start_date,
        end_date = EXCLUDED.end_date;

-- 5. SEED QUALIFICATIONS / CERTIFICATIONS
INSERT INTO qualifications (id, user_id, title, organization, issue_date, expiration_date, credential_id, credential_url)
VALUES
    (
        '44444444-4444-4444-4444-444444444401',
        '11111111-1111-1111-1111-111111111111',
        'AWS Certified Solutions Architect - Associate',
        'Amazon Web Services (AWS)',
        '2023-05-15',
        '2026-05-15',
        'AWS-SAA-839219',
        'https://aws.amazon.com/verification/AWS-SAA-839219'
    ),
    (
        '44444444-4444-4444-4444-444444444402',
        '11111111-1111-1111-1111-111111111111',
        'Oracle Certified Professional: Java SE 17 Developer',
        'Oracle',
        '2022-10-10',
        NULL,
        'OCP-JAVA17-10293',
        'https://catalog-education.oracle.com/pls/certview/sharebadge?id=OCP-JAVA17-10293'
    ),
    (
        '44444444-4444-4444-4444-444444444403',
        '11111111-1111-1111-1111-111111111112',
        'Google UX Design Professional Certificate',
        'Google',
        '2021-08-20',
        NULL,
        'GGL-UX-49102',
        'https://coursera.org/verify/professional-cert/GGL-UX-49102'
    )
ON CONFLICT (id) DO UPDATE
    SET title = EXCLUDED.title,
        organization = EXCLUDED.organization,
        issue_date = EXCLUDED.issue_date,
        expiration_date = EXCLUDED.expiration_date,
        credential_id = EXCLUDED.credential_id,
        credential_url = EXCLUDED.credential_url;

-- 6. SEED ATTACHED SKILLS FOR USERS (SKILLS_USERS)
INSERT INTO skills_users (user_id, skill_id, is_top_skill, proficiency_level, years_of_experience)
VALUES
    -- Nguyen Van An's Skills
    ('11111111-1111-1111-1111-111111111111', '55555555-5555-5555-5555-555555550001', TRUE, 'EXPERT', 6), -- Java
    ('11111111-1111-1111-1111-111111111111', '55555555-5555-5555-5555-555555550002', TRUE, 'EXPERT', 5), -- Spring Boot
    ('11111111-1111-1111-1111-111111111111', '55555555-5555-5555-5555-555555550011', TRUE, 'ADVANCED', 5), -- PostgreSQL
    ('11111111-1111-1111-1111-111111111111', '55555555-5555-5555-5555-555555550014', FALSE, 'ADVANCED', 4), -- Docker
    ('11111111-1111-1111-1111-111111111111', '55555555-5555-5555-5555-555555550006', FALSE, 'INTERMEDIATE', 3), -- React
    ('11111111-1111-1111-1111-111111111111', '55555555-5555-5555-5555-555555550021', FALSE, 'ADVANCED', 3), -- RabbitMQ

    -- Tran Thi Mai's Skills
    ('11111111-1111-1111-1111-111111111112', '55555555-5555-5555-5555-555555550018', TRUE, 'EXPERT', 5), -- Figma
    ('11111111-1111-1111-1111-111111111112', '55555555-5555-5555-5555-555555550019', TRUE, 'EXPERT', 6), -- UI/UX Design
    ('11111111-1111-1111-1111-111111111112', '55555555-5555-5555-5555-555555550020', FALSE, 'ADVANCED', 4) -- Wireframing & Prototyping
ON CONFLICT (user_id, skill_id) DO UPDATE
    SET is_top_skill = EXCLUDED.is_top_skill,
        proficiency_level = EXCLUDED.proficiency_level,
        years_of_experience = EXCLUDED.years_of_experience;
