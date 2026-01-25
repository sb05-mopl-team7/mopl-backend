CREATE TABLE users
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    name              VARCHAR(255) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    password          VARCHAR(255) NULL,
    role              VARCHAR(10)  NOT NULL,
    locked            BOOLEAN      NOT NULL DEFAULT FALSE,
    profile_image_url VARCHAR(255) NULL,
    provider          VARCHAR(255) NULL,
    provider_id       VARCHAR(255) NULL,
    updated_at        DATETIME     NOT NULL,
    created_at        DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
);

CREATE TABLE contents
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    content_type   VARCHAR(255) NOT NULL,
    origin_id      BIGINT       NULL,
    title          VARCHAR(255) NOT NULL,
    description    TEXT         NOT NULL,
    thumbnail_url  VARCHAR(255) NOT NULL,
    average_rating DOUBLE DEFAULT 0.0,
    review_count   INT    DEFAULT 0,
    created_at        DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_content_origin (content_type, origin_id)
);

CREATE TABLE tags
(
    id  BIGINT       NOT NULL AUTO_INCREMENT,
    tag VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tags_tag (tag)
);

CREATE TABLE content_tags
(
    id         BIGINT NOT NULL AUTO_INCREMENT,
    content_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_content_tag (content_id, tag_id)
);

CREATE TABLE reviews
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    content_id BIGINT       NOT NULL,
    texts      VARCHAR(255) NOT NULL,
    rating     DOUBLE       NOT NULL,
    updated_at DATETIME     NOT NULL,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reviews_user_content (user_id, content_id),
    -- 콘텐츠별 최신 리뷰 조회를 위한 인덱스
    INDEX idx_review_content_created (content_id, created_at DESC)
);

CREATE TABLE playlists
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    title            VARCHAR(255) NOT NULL,
    description      VARCHAR(255) NOT NULL,
    subscriber_count BIGINT       NOT NULL DEFAULT 0,
    updated_at       DATETIME     NOT NULL,
    created_at       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    -- 유저별 플레이리스트 최신순 조회를 위한 인덱스
    INDEX idx_playlist_user_created (user_id, created_at DESC)
);

CREATE TABLE playlist_subscribes
(
    id          BIGINT NOT NULL AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    playlist_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_playlist_subscribes (user_id, playlist_id)
);

CREATE TABLE playlist_contents
(
    id          BIGINT NOT NULL AUTO_INCREMENT,
    playlist_id BIGINT NOT NULL,
    content_id  BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_playlist_contents (playlist_id, content_id)
);

CREATE TABLE conversations
(
    id              BIGINT   NOT NULL AUTO_INCREMENT,
    created_at      DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE read_status
(
    id              BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    last_message_id BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_read_status_conversation_user (conversation_id, user_id)
);

CREATE TABLE direct_messages
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT       NOT NULL,
    author_id       BIGINT       NOT NULL,
    content         VARCHAR(255) NOT NULL,
    created_at      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    -- 대화방별 메시지 최신순 조회를 위한 인덱스
    INDEX idx_dm_conv_created (conversation_id, created_at DESC)
);

CREATE TABLE notifications
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    receiver_id BIGINT       NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     VARCHAR(255) NOT NULL,
    level       VARCHAR(255) NOT NULL,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    -- 유저별 알림 최신순 조회를 위한 인덱스
    INDEX idx_noti_receiver_created (receiver_id, created_at DESC)
);

CREATE TABLE follows
(
    id          BIGINT NOT NULL AUTO_INCREMENT,
    follower_id BIGINT NOT NULL,
    followee_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_follows_relation (follower_id, followee_id)
);