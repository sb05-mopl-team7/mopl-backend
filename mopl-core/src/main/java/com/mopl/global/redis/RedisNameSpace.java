package com.mopl.global.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Getter
@RequiredArgsConstructor
public enum RedisNameSpace {
    // 인증 토큰
    AUTH_TOKEN("token:", Duration.ofDays(7), false),

    // 임시 비밀번호
    TEMP_PASSWORD("temp-password:", Duration.ofMinutes(3), true),

    // DM 채팅방 접속자 명단 Set
    DM_VIEWERS("dm-chat:viewer:", Duration.ofHours(2), true),

    // 세션용
    CONTENT_SESSIONS("content:sessions:", Duration.ofHours(1), true);

    private final String prefix;
    private final Duration ttl;
    private final boolean evictable;

    public String createKey(String identifier) {
        return this.prefix + identifier;
    }
}
