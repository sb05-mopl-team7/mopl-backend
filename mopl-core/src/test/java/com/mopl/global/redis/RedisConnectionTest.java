package com.mopl.global.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
})
class RedisConnectionTest {

    @Autowired
    private RedisManager redisManager;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Autowired
    private RedisTemplate redisTemplate;

    private final String TEST_ID = "testUser@mopl.com";
    private final String TEST_VALUE = "password123412";
    private final RedisNameSpace NAMESPACE = RedisNameSpace.TEMP_PASSWORD;

    @Test
    @Order(1)
    @DisplayName("1. Redis 서버 연결 테스트")
    void connectionTest() {
        // Redis 서버와 실제로 통신이 가능한지만 확인
        assertThat(connectionFactory.getConnection().ping()).isEqualTo("PONG");
    }

    @Test
    @Order(2)
    @DisplayName("2. 데이터 저장 및 조회 테스트")
    void saveAndFindTest() {
        // given
        redisManager.save(NAMESPACE, TEST_ID, TEST_VALUE);

        // when
        Optional<String> result = redisManager.findByKey(NAMESPACE, TEST_ID, String.class);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(TEST_VALUE);
        System.out.println(result.get());
    }

    @Test
    @Order(3)
    @DisplayName("3. 데이터 삭제 테스트")
    void deleteTest() {
        // given
        redisManager.save(NAMESPACE, TEST_ID, TEST_VALUE);

        // when
        redisManager.delete(NAMESPACE, TEST_ID);
        boolean exists = redisManager.hasKey(NAMESPACE, TEST_ID);

        // then
        assertThat(exists).isFalse();
    }
}