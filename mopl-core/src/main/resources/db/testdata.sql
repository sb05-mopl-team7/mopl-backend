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

-- 샘플 데이터 20개 삽입
INSERT INTO notifications (receiver_id, title, content, level, created_at)
VALUES (1, '새로운 메시지 도착', '김철수님이 메시지를 보냈습니다.', 'INFO', '2026-01-10 09:00:00'),
       (2, '결제 완료', '주문번호 #12345의 결제가 완료되었습니다.', 'ERROR', '2026-01-10 09:15:00'),
       (1, '비밀번호 변경 필요', '마지막 비밀번호 변경 후 90일이 경과했습니다.', 'WARNING', '2026-01-10 09:30:00'),
       (3, '로그인 실패', '5회 이상 로그인 시도가 실패했습니다.', 'ERROR', '2026-01-10 09:45:00'),
       (2, '회원가입 축하', '회원가입을 환영합니다!', 'ERROR', '2026-01-10 10:00:00'),
       (4, '새로운 댓글', '작성하신 게시글에 새로운 댓글이 달렸습니다.', 'INFO', '2026-01-10 10:15:00'),
       (5, '서버 점검 안내', '1월 15일 새벽 2시~4시 서버 점검이 예정되어 있습니다.', 'WARNING', '2026-01-10 10:30:00'),
       (3, '파일 업로드 완료', '문서.pdf 파일이 성공적으로 업로드되었습니다.', 'ERROR', '2026-01-10 10:45:00'),
       (1, '디스크 용량 부족', '저장공간이 90% 이상 사용되었습니다.', 'ERROR', '2026-01-10 11:00:00'),
       (6, '친구 요청', '이영희님이 친구 요청을 보냈습니다.', 'INFO', '2026-01-10 11:15:00'),
       (4, '이메일 인증 필요', '이메일 인증을 완료해주세요.', 'WARNING', '2026-01-10 11:30:00'),
       (7, '주문 배송 시작', '주문하신 상품이 배송 시작되었습니다.', 'ERROR', '2026-01-10 11:45:00'),
       (2, '결제 실패', '카드 승인이 거부되었습니다.', 'ERROR', '2026-01-10 12:00:00'),
       (8, '이벤트 당첨', '축하합니다! 추첨 이벤트에 당첨되셨습니다.', 'ERROR', '2026-01-10 12:15:00'),
       (5, '약관 변경 안내', '개인정보 처리방침이 변경되었습니다.', 'INFO', '2026-01-10 12:30:00'),
       (9, '계정 보안 경고', '새로운 기기에서 로그인이 감지되었습니다.', 'ERROR', '2026-01-10 12:45:00'),
       (6, '데이터 백업 실패', '자동 백업 프로세스가 실패했습니다.', 'ERROR', '2026-01-10 13:00:00'),
       (10, '좋아요 알림', '박민수님이 회원님의 게시물을 좋아합니다.', 'INFO', '2026-01-10 13:15:00'),
       (7, '쿠폰 발급', '신규 회원 10% 할인 쿠폰이 발급되었습니다.', 'WARNING', '2026-01-10 13:30:00'),
       (8, 'API 사용량 초과', '일일 API 호출 한도의 80%를 초과했습니다.', 'WARNING', '2026-01-10 13:45:00');
-- 샘플 데이터 5개 삽입
INSERT INTO contents (content_type, title, description, thumbnail_url, average_rating, review_count, created_at)
VALUES ('movie', '인터스텔라', '인류의 생존을 위해 우주로 떠나는 탐험가들의 이야기를 그린 SF 대작', 'https://example.com/interstellar.jpg', 4.8, 15234,'2026-01-10 10:00:00'),
       ('movie', '해리포터와 마법사의 돌', 'J.K. 롤링의 판타지 소설 시리즈 첫 번째 작품', 'https://example.com/harry-potter.jpg', 4.7, 28901,'2026-01-10 11:00:00'),
       ('movie', '젤다의 전설: 브레스 오브 더 와일드', '광활한 오픈 월드를 탐험하는 액션 어드벤처 게임', 'https://example.com/zelda.jpg', 4.9, 42156,'2026-01-10 12:00:00'),
       ('movie', 'Bohemian Rhapsody', '퀸의 대표곡으로 록 오페라 스타일의 명곡', 'https://example.com/bohemian.jpg', 4.9, 67823,'2026-01-10 13:00:00'),
       ('movie', '브레이킹 배드', '화학 교사가 마약 제조자로 변해가는 과정을 그린 범죄 드라마', 'https://example.com/breaking-bad.jpg', 4.8, 51290,'2026-01-10 14:00:00');