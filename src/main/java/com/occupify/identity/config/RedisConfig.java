package com.occupify.identity.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    private static final String REVOKE_ALL_SESSIONS_LUA = """
            local sessionKey = KEYS[1]
            local tokens = redis.call("SMEMBERS", sessionKey)
            local count = 0
            for _, tokenKey in ipairs(tokens) do
                redis.call("DEL", tokenKey)
                count = count + 1
            end
            redis.call("DEL", sessionKey)
            return count
            """;

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    public RedisScript<Long> revokeAllSessionsScript() {
        return new DefaultRedisScript<>(REVOKE_ALL_SESSIONS_LUA, Long.class);
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}
