CREATE DATABASE IF NOT EXISTS mopl_local;
USE mopl_local;

-- 1. 기초 데이터 초기화 (외래 키 제약 조건 잠시 해제 후 청소)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE users;
TRUNCATE TABLE contents;
TRUNCATE TABLE conversations;
TRUNCATE TABLE direct_messages;
TRUNCATE TABLE notifications;
TRUNCATE TABLE reviews;
TRUNCATE TABLE playlists;
SET FOREIGN_KEY_CHECKS = 1;

-- 2. 필수 부모 데이터 생성
INSERT INTO users (id, name, email, role, locked, updated_at, created_at)
VALUES (1, '테스터', 'test@mopl.io', 'USER', false, NOW(), NOW());

INSERT INTO contents (id, content_type, title, description, thumbnail_url, created_at)
VALUES (1, 'movie', '인터스텔라', '우주 SF 영화', 'http://image.com', NOW());

INSERT INTO conversations (id, created_at)
VALUES (1, NOW());

-- 3. 데이터 삽입 프로시저 정의 (* 데이터 갯수는 늘려서 해주세요 코드잇에서 빌린 맥북 성능이 별로라 멈추네요)

-- ① DM 더미 데이터 (1만 건)
DROP PROCEDURE IF EXISTS insert_dm_data;
DELIMITER $$
CREATE PROCEDURE insert_dm_data()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 10000 DO
            INSERT INTO direct_messages (conversation_id, author_id, content, created_at)
            VALUES (1, 1, CONCAT('DM 메시지입니다 - ', i), DATE_ADD('2026-01-01 00:00:00', INTERVAL i SECOND));
            SET i = i + 1;
        END WHILE;
END$$
DELIMITER ;

-- ② 알림 더미 데이터 (1만 건)
DROP PROCEDURE IF EXISTS insert_notification_data;
DELIMITER $$
CREATE PROCEDURE insert_notification_data()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 10000 DO
            INSERT INTO notifications (receiver_id, title, content, level, created_at)
            VALUES (1, '알림 제목', '알림 내용입니다.', 'INFO', DATE_ADD('2026-01-01 00:00:00', INTERVAL i SECOND));
            SET i = i + 1;
        END WHILE;
END$$
DELIMITER ;

-- ③ 리뷰 더미 데이터 (5천 건)
DROP PROCEDURE IF EXISTS insert_review_data;
DELIMITER $$
CREATE PROCEDURE insert_review_data()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 5000 DO
            INSERT INTO reviews (user_id, content_id, texts, rating, updated_at, created_at)
            VALUES (i, 1, CONCAT('리뷰 텍스트 - ', i), 4.0, NOW(), DATE_ADD('2026-01-01 00:00:00', INTERVAL i MINUTE));
            SET i = i + 1;
        END WHILE;
END$$
DELIMITER ;

-- ④ 플레이리스트 더미 데이터 (3천 건)
DROP PROCEDURE IF EXISTS insert_playlist_data;
DELIMITER $$
CREATE PROCEDURE insert_playlist_data()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 3000 DO
            INSERT INTO playlists (user_id, title, description, subscriber_count, updated_at, created_at)
            VALUES (1, CONCAT('플레이리스트 ', i), '설명', 0, NOW(), NOW());
            SET i = i + 1;
        END WHILE;
END$$
DELIMITER ;

-- 4. 프로시저 실행 (실제 데이터 생성)
SELECT 'DM 데이터 삽입 시작...' AS msg;
CALL insert_dm_data();

SELECT '알림 데이터 삽입 시작...' AS msg;
CALL insert_notification_data();

SELECT '리뷰 데이터 삽입 시작...' AS msg;
CALL insert_review_data();

SELECT '플레이리스트 데이터 삽입 시작...' AS msg;
CALL insert_playlist_data();

SELECT '모든 더미 데이터 생성이 완료되었습니다.' AS msg;

SHOW TABLES;