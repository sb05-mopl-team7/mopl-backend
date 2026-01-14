package com.mopl.domain.watching.entity;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator; // 추가
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;

@Getter
@RedisHash(value = "watching_session", timeToLive = 3600) // 1시간뒤 자동 만료
public class WatchingSession {

    @Id
    private final Long id; // watcherId 역할을 함

    private final Long contentId;

    private final LocalDateTime createdAt;

    @Builder
    @PersistenceCreator // Spring Data Redis가 데이터를 복원할 때 이 생성자를 사용하도록 명시
    public WatchingSession(Long id, Long contentId, LocalDateTime createdAt) {
        this.id = id;
        this.contentId = contentId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}