-- Seed initial accounts: user@gmail.com and admin@gmail.com (Password: password123)
-- BCrypt hash for "password123": $2a$10$gOtSD9sxE43pXEdR0W.UaOJUZIqAiIgI7f01BudsAIFsC6AJfnasq
INSERT INTO users (email, password, role, status)
VALUES 
    ('user@gmail.com', '$2a$10$gOtSD9sxE43pXEdR0W.UaOJUZIqAiIgI7f01BudsAIFsC6AJfnasq', 'USER', 'ACTIVE'),
    ('admin@gmail.com', '$2a$10$gOtSD9sxE43pXEdR0W.UaOJUZIqAiIgI7f01BudsAIFsC6AJfnasq', 'ADMIN', 'ACTIVE')
ON CONFLICT (email) DO NOTHING;
