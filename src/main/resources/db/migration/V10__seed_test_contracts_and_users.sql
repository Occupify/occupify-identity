-- =============================================================================
-- Migration: V10__seed_test_contracts_and_users.sql
-- Service: occupify-identity
-- Description: Seed test users and contract replicas for cross-service sync
-- =============================================================================

-- 1. Ensure test users exist in identity
INSERT INTO users (id, email, password_hash, full_name, role, status, created_at, updated_at)
VALUES
    (
        '00000000-0000-0000-0000-000000000001',
        'dev-employer@occupify.local',
        '$2a$10$gOtSD9sxE43pXEdR0W.UaOJUZIqAiIgI7f01BudsAIFsC6AJfnasq',
        'Tôi (Chủ dự án)',
        'USER',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '33333333-3333-3333-3333-333333333333',
        'khoa.nguyen@gmail.com',
        '$2a$10$gOtSD9sxE43pXEdR0W.UaOJUZIqAiIgI7f01BudsAIFsC6AJfnasq',
        'Nguyễn Minh Khoa',
        'USER',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (id) DO NOTHING;

-- 2. Seed Contract Replicas (c1 to c6)
INSERT INTO contracts (
    id,
    project_role_id,
    employer_id,
    employee_id,
    source_type,
    source_id,
    agreed_rate,
    salary_type,
    status,
    payment_status,
    started_at,
    finished_at,
    title,
    role,
    freelancer_email,
    created_at,
    updated_at
)
VALUES
    -- Contract c1 (PENDING)
    (
        'c1111111-1111-1111-1111-111111111111',
        'b0000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000001',
        '33333333-3333-3333-3333-333333333333',
        'JOB_BID',
        '66666666-6666-6666-6666-666666666661',
        20000000.00,
        'MONTHLY',
        'PENDING',
        'PENDING',
        NULL,
        NULL,
        'Senior Backend Engineer - Core Payments',
        'Backend Developer',
        'khoa.nguyen@gmail.com',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),

    -- Contract c2 (IN_PROGRESS)
    (
        'c2222222-2222-2222-2222-222222222222',
        'b0000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000001',
        '33333333-3333-3333-3333-333333333333',
        'JOB_BID',
        '66666666-6666-6666-6666-666666666662',
        15000000.00,
        'MONTHLY',
        'IN_PROGRESS',
        'ESCROW_LOCKED',
        CURRENT_TIMESTAMP - INTERVAL '14 days',
        NULL,
        'Frontend Developer - Mobile App',
        'Frontend Developer',
        'khoa.nguyen@gmail.com',
        CURRENT_TIMESTAMP - INTERVAL '14 days',
        CURRENT_TIMESTAMP
    ),

    -- Contract c3 (IN_PROGRESS)
    (
        'c3333333-3333-3333-3333-333333333333',
        'b0000000-0000-0000-0000-000000000003',
        '00000000-0000-0000-0000-000000000001',
        '33333333-3333-3333-3333-333333333333',
        'JOB_BID',
        '66666666-6666-6666-6666-666666666663',
        12000000.00,
        'MONTHLY',
        'IN_PROGRESS',
        'ESCROW_LOCKED',
        CURRENT_TIMESTAMP - INTERVAL '7 days',
        NULL,
        'UI/UX Designer - Design System',
        'UI/UX Designer',
        'khoa.nguyen@gmail.com',
        CURRENT_TIMESTAMP - INTERVAL '7 days',
        CURRENT_TIMESTAMP
    ),

    -- Contract c4 (IN_PROGRESS - Dev Mock as Freelancer)
    (
        'c4444444-4444-4444-4444-444444444444',
        'b0000000-0000-0000-0000-000000000005',
        '11111111-1111-1111-1111-111111111111',
        '00000000-0000-0000-0000-000000000001',
        'JOB_BID',
        '66666666-6666-6666-6666-666666666665',
        20000000.00,
        'MONTHLY',
        'IN_PROGRESS',
        'ESCROW_LOCKED',
        CURRENT_TIMESTAMP - INTERVAL '5 days',
        NULL,
        'Frontend Engineer - React Component Library',
        'Frontend Engineer',
        'dev-employer@occupify.local',
        CURRENT_TIMESTAMP - INTERVAL '5 days',
        CURRENT_TIMESTAMP
    ),

    -- Contract c5 (COMPLETED)
    (
        'c5555555-5555-5555-5555-555555555555',
        'b0000000-0000-0000-0000-000000000004',
        '00000000-0000-0000-0000-000000000001',
        '33333333-3333-3333-3333-333333333333',
        'JOB_BID',
        '66666666-6666-6666-6666-666666666664',
        10000000.00,
        'MONTHLY',
        'COMPLETED',
        'PAID',
        CURRENT_TIMESTAMP - INTERVAL '30 days',
        CURRENT_TIMESTAMP - INTERVAL '1 day',
        'QA / Tester Lead - Automation Suite',
        'QA / Tester Lead',
        'khoa.nguyen@gmail.com',
        CURRENT_TIMESTAMP - INTERVAL '30 days',
        CURRENT_TIMESTAMP - INTERVAL '1 day'
    ),

    -- Contract c6 (PENDING)
    (
        'c6666666-6666-6666-6666-666666666666',
        'b0000000-0000-0000-0000-000000000006',
        '11111111-1111-1111-1111-111111111111',
        '00000000-0000-0000-0000-000000000001',
        'JOB_BID',
        '66666666-6666-6666-6666-666666666666',
        25000000.00,
        'MONTHLY',
        'PENDING',
        'PENDING',
        NULL,
        NULL,
        'UI/UX Lead - SaaS Tokens & Guidelines',
        'UI/UX Lead',
        'dev-employer@occupify.local',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (id) DO NOTHING;
