INSERT INTO users (email, password, role, status)
VALUES 
    ('user@gmail.com', '$2a$10$gOtSD9sxE43pXEdR0W.UaOJUZIqAiIgI7f01BudsAIFsC6AJfnasq', 'USER', 'ACTIVE'),
    ('admin@gmail.com', '$2a$10$gOtSD9sxE43pXEdR0W.UaOJUZIqAiIgI7f01BudsAIFsC6AJfnasq', 'ADMIN', 'ACTIVE')
ON CONFLICT (email) DO UPDATE 
    SET password = EXCLUDED.password,
        status = EXCLUDED.status;
