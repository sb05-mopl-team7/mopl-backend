package com.mopl.global.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisManager {

    private final RedisTemplate<String, Object> redisTemplate;

    /** 데이터 저장 - 네임스페이스에 정의된 TTL 적용 */
    public void save(RedisNameSpace namespace, String identifier, Object value) {
        String key = namespace.createKey(identifier);
        redisTemplate.opsForValue().set(key, value, namespace.getTtl());
    }

    /** 데이터 조회 */
    public <T> Optional<T> findByKey(RedisNameSpace namespace, String identifier, Class<T> clazz) {
        String key = namespace.createKey(identifier);
        Object value = redisTemplate.opsForValue().get(key);

        if(value == null) return Optional.empty();

        try {
            return Optional.of(clazz.cast(value));
        } catch (ClassCastException e) {
            log.error("Redis 타입 불일치 발생 - Key: {}, 예상 타입: {}, 실제 데이터 타입: {}",
                    key, clazz.getSimpleName(), value.getClass().getSimpleName());
            return Optional.empty();}
    }

    /** 데이터 삭제 */
    public void delete(RedisNameSpace namespace, String identifier) {
        String key = namespace.createKey(identifier);
        redisTemplate.delete(key);
    }

    /** 키 존재 여부 확인 */
    public boolean hasKey(RedisNameSpace namespace, String identifier) {
        String key = namespace.createKey(identifier);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
