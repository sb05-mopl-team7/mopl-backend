-- 비밀번호는 모두 'password1234'를 BCrypt 암호화한 값입니다.

INSERT INTO users (name, email, password, role, locked, profile_image_url, provider, provider_id, created_at,
                   updated_at)
VALUES ('김철수', 'chulsoo@example.com', '$2a$10$mYN8aj6ZILN9q9ZhMZOYB.1tbCNc4qw/KPyvmoDnonz.XMG7Ng7l6', 'USER', false,
        'https://picsum.photos/200', null, null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('이영희', 'younghee@example.com', '$2a$10$mYN8aj6ZILN9q9ZhMZOYB.1tbCNc4qw/KPyvmoDnonz.XMG7Ng7l6', 'USER', false,
        null, 'local', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('관리자', 'admin@mopl.com', '$2a$10$mYN8aj6ZILN9q9ZhMZOYB.1tbCNc4qw/KPyvmoDnonz.XMG7Ng7l6', 'ADMIN', false, null,
        'local', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('박지민', 'jimin@example.com', '$2a$10$mYN8aj6ZILN9q9ZhMZOYB.1tbCNc4qw/KPyvmoDnonz.XMG7Ng7l6', 'USER', false,
        'https://picsum.photos/201', null, '12345678', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('최유진', 'yujin@example.com', '$2a$10$mYN8aj6ZILN9q9ZhMZOYB.1tbCNc4qw/KPyvmoDnonz.XMG7Ng7l6', 'USER', true, null,
        null, null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);