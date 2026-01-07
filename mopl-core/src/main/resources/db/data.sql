-- 비밀번호는 모두 'password123!'를 BCrypt 암호화한 값입니다.
-- $2a$10$8KzS1A4y5Wp.fX3XWqLpS.ZfF9v7K9Y1V5zJ9S.y9Z1X5zJ9S.y9Z

INSERT INTO users (name, email, password, role, locked, profile_image_url, provider, provider_id, created_at,
                   updated_at)
VALUES ('김철수', 'chulsoo@example.com', '$2a$10$8KzS1A4y5Wp.fX3XWqLpS.ZfF9v7K9Y1V5zJ9S.y9Z1X5zJ9S.y9Z', 'USER', false,
        'https://picsum.photos/200', 'local', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('이영희', 'younghee@example.com', '$2a$10$8KzS1A4y5Wp.fX3XWqLpS.ZfF9v7K9Y1V5zJ9S.y9Z1X5zJ9S.y9Z', 'USER', false,
        null, 'local', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('관리자', 'admin@mopl.com', '$2a$10$8KzS1A4y5Wp.fX3XWqLpS.ZfF9v7K9Y1V5zJ9S.y9Z1X5zJ9S.y9Z', 'ADMIN', false, null,
        'local', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('박지민', 'jimin@example.com', '$2a$10$8KzS1A4y5Wp.fX3XWqLpS.ZfF9v7K9Y1V5zJ9S.y9Z1X5zJ9S.y9Z', 'USER', false,
        'https://picsum.photos/201', 'kakao', '12345678', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('최유진', 'yujin@example.com', '$2a$10$8KzS1A4y5Wp.fX3XWqLpS.ZfF9v7K9Y1V5zJ9S.y9Z1X5zJ9S.y9Z', 'USER', true, null,
        'local', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('정민호', 'minho@example.com', '$2a$10$8KzS1A4y5Wp.fX3XWqLpS.ZfF9v7K9Y1V5zJ9S.y9Z1X5zJ9S.y9Z', 'USER', false,
        'https://picsum.photos/202', 'google', '98765432', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('강서연', 'seoyeon@example.com', '$2a$10$8KzS1A4y5Wp.fX3XWqLpS.ZfF9v7K9Y1V5zJ9S.y9Z1X5zJ9S.y9Z', 'USER', false,
        null, 'local', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('윤도현', 'dohyun@example.com', '$2a$10$8KzS1A4y5Wp.fX3XWqLpS.ZfF9v7K9Y1V5zJ9S.y9Z1X5zJ9S.y9Z', 'USER', false,
        'https://picsum.photos/203', 'local', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('한소희', 'sohee@example.com', '$2a$10$8KzS1A4y5Wp.fX3XWqLpS.ZfF9v7K9Y1V5zJ9S.y9Z1X5zJ9S.y9Z', 'USER', false, null,
        'local', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('임재범', 'jaebeom@example.com', '$2a$10$8KzS1A4y5Wp.fX3XWqLpS.ZfF9v7K9Y1V5zJ9S.y9Z1X5zJ9S.y9Z', 'USER', false,
        'https://picsum.photos/204', 'naver', '55667788', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);