package com.mopl.global.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Getter
@RequiredArgsConstructor
public enum RedisNameSpace {
    // 인증 토큰
    AUTH_TOKEN("token", Duration.ofDays(7), false),

    // 임시 비밀번호
    TEMP_PASSWORD("temp-password", Duration.ofMinutes(3), true),

    // DM 채팅방 접속자 명단 Set
    DM_VIEWERS("dm-chat:viewer", Duration.ofHours(2), true),

    // 실시간 시청자 수 (중복 제거 및 정리)
    WATCHER_COUNT("watcher", Duration.ofHours(1), false),

    // 특정 유저가 시청 중인 세션 정보 (Hash 구조)
    USER_WATCHING("user-watching", Duration.ofHours(2), true),

    // 특정 콘텐츠를 시청 중인 유저 ID 목록 (Set 구조)
    CONTENT_WATCHERS("content-watchers", Duration.ofHours(2), true);

    private final String prefix;
    private final Duration ttl;
    private final boolean evictable;

    /**
     * 네임스페이스 프리픽스와 식별자를 결합하여 Redis Key 생성
     */
    public String createKey(String identifier) {
        return String.format("%s:%s", this.prefix, identifier);
    }
}