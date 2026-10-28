package com.occupify.identity.config;

import com.occupify.identity.constant.RedisConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("RedisConfig Unit Tests")
class RedisConfigTest {

    private final RedisConfig redisConfig = new RedisConfig();
    private final RedisConnectionFactory mockConnectionFactory = mock(RedisConnectionFactory.class);

    @Test
    @DisplayName("stringRedisTemplate -> bean created successfully")
    void stringRedisTemplate_shouldBeCreated() {
        StringRedisTemplate template = redisConfig.stringRedisTemplate(mockConnectionFactory);
        assertThat(template).isNotNull();
    }

    @Test
    @DisplayName("revokeAllSessionsScript -> bean created successfully with Long resultType")
    void revokeAllSessionsScript_shouldBeCreated() {
        RedisScript<Long> script = redisConfig.revokeAllSessionsScript();
        assertThat(script).isNotNull();
        assertThat(script.getResultType()).isEqualTo(Long.class);
    }

    @Test
    @DisplayName("cacheManager -> creates RedisCacheManager with expected configuration")
    void cacheManager_shouldBeCreated() {
        RedisCacheManager cacheManager = redisConfig.cacheManager(mockConnectionFactory);
        assertThat(cacheManager).isNotNull();
    }

    @Test
    @DisplayName("cache serialization -> test UserProfileResponse serialization with JdkSerializationRedisSerializer")
    void cacheSerialization_test() {
        org.springframework.data.redis.serializer.JdkSerializationRedisSerializer serializer =
                new org.springframework.data.redis.serializer.JdkSerializationRedisSerializer();
        com.occupify.identity.dto.response.user.UserProfileResponse response =
                new com.occupify.identity.dto.response.user.UserProfileResponse(
                        java.util.UUID.randomUUID(),
                        "user@gmail.com",
                        "Demo User",
                        "Developer",
                        null,
                        null,
                        null,
                        "USER",
                        "ACTIVE",
                        java.time.Instant.now(),
                        java.time.Instant.now()
                );
        byte[] bytes = serializer.serialize(response);
        Object deserialized = serializer.deserialize(bytes);
        assertThat(deserialized).isEqualTo(response);
    }
}
