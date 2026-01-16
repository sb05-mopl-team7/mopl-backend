USE mopl_local;

-- 1. Direct Messages (DM) 성능 테스트
-- [BEFORE]
EXPLAIN SELECT * FROM direct_messages WHERE conversation_id = 1 ORDER BY created_at DESC LIMIT 20;

-- [INDEX 생성]
CREATE INDEX idx_dm_conv_created ON direct_messages (conversation_id, created_at DESC);

-- [AFTER]
EXPLAIN SELECT * FROM direct_messages WHERE conversation_id = 1 ORDER BY created_at DESC LIMIT 20;


-- 2. Notifications (알림) 성능 테스트
-- [BEFORE]
EXPLAIN SELECT * FROM notifications WHERE receiver_id = 1 ORDER BY created_at DESC LIMIT 20;

-- [INDEX 생성]
CREATE INDEX idx_noti_receiver_created ON notifications (receiver_id, created_at DESC);

-- [AFTER]
EXPLAIN SELECT * FROM notifications WHERE receiver_id = 1 ORDER BY created_at DESC LIMIT 20;


-- 3. Reviews (리뷰) 성능 테스트
-- [BEFORE]
EXPLAIN SELECT * FROM reviews WHERE content_id = 1 ORDER BY created_at DESC LIMIT 20;

-- [INDEX 생성]
CREATE INDEX idx_review_content_created ON reviews (content_id, created_at DESC);

-- [AFTER]
EXPLAIN SELECT * FROM reviews WHERE content_id = 1 ORDER BY created_at DESC LIMIT 20;


-- 4. Playlists (플레이리스트) 성능 테스트
-- [BEFORE]
EXPLAIN SELECT * FROM playlists WHERE user_id = 1 ORDER BY created_at DESC LIMIT 20;

-- [INDEX 생성]
CREATE INDEX idx_playlist_user_created ON playlists (user_id, created_at DESC);

-- [AFTER]
EXPLAIN SELECT * FROM playlists WHERE user_id = 1 ORDER BY created_at DESC LIMIT 20;

SHOW TABLES;


-- 인덱스 삭제가 필요할 때 (초기화)
-- DROP INDEX idx_dm_conv_created ON direct_messages;
-- DROP INDEX idx_noti_receiver_created ON notifications;
-- DROP INDEX idx_review_content_created ON reviews;
-- DROP INDEX idx_playlist_user_created ON playlists;