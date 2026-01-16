package com.mopl.global.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    /** Set 구조에 데이터 추가 */
    public void addToSet(RedisNameSpace namespace, String identifier, Object value) {
        String key = namespace.createKey(identifier);
        redisTemplate.opsForSet().add(key, value);
        redisTemplate.expire(key, namespace.getTtl());
    }

    /** Set 구조에서 데이터 삭제 */
    public void removeFromSet(RedisNameSpace namespace, String identifier, Object value) {
        String key = namespace.createKey(identifier);
        redisTemplate.opsForSet().remove(key, value);
    }

    /** Set 전체 멤버 조회 */
    public <T> Set<T> getSetMembers(RedisNameSpace namespace, String identifier, Class<T> clazz) {
        String key = namespace.createKey(identifier);
        Set<Object> members = redisTemplate.opsForSet().members(key);

        if (members == null) return Collections.emptySet();

        return members.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .collect(Collectors.toSet());
    }

    /** Hash 구조로 객체 저장 */
    public void saveHash(RedisNameSpace namespace, String identifier, Object value) {
        String key = namespace.createKey(identifier);
        // 객체를 Hash 맵으로 변환하여 저장 (Jackson 등이 자동으로 처리)
        redisTemplate.opsForHash().putAll(key, new ObjectMapper().convertValue(value, Map.class));
        redisTemplate.expire(key, namespace.getTtl());
    }

    /** Hash 구조에서 객체 조회 */
    public <T> Optional<T> findHashByKey(RedisNameSpace namespace, String identifier, Class<T> clazz) {
        String key = namespace.createKey(identifier);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (entries.isEmpty()) return Optional.empty();

        return Optional.of(new ObjectMapper().convertValue(entries, clazz));
    }
}
