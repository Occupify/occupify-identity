package com.occupify.identity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.identity.service.impl.RedisSessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisSessionServiceImplTest {

        @Mock
        private ReactiveRedisOperations<String, String> redisTemplate;

        @Mock
        private ReactiveValueOperations<String, String> valueOperations;

        @Mock
        private ReactiveSetOperations<String, String> setOperations;

        @Mock
        private RedisScript<Long> revokeAllSessionsScript;

        private ObjectMapper objectMapper;
        private RedisSessionServiceImpl sessionService;

        @BeforeEach
        void setUp() {
                objectMapper = new ObjectMapper();
                sessionService = new RedisSessionServiceImpl(
                                redisTemplate,
                                revokeAllSessionsScript,
                                objectMapper,
                                604800000L);
        }

        @Test
        void shouldSaveSessionSuccessfully() {
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(redisTemplate.opsForSet()).thenReturn(setOperations);
                when(valueOperations.set(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
                when(setOperations.add(anyString(), anyString())).thenReturn(Mono.just(1L));
                when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));

                UUID userId = UUID.randomUUID();
                StepVerifier.create(sessionService.saveSession("refresh-token-123", userId, "user@occupify.com"))
                                .verifyComplete();

                verify(valueOperations).set(eq("refresh_token:refresh-token-123"), anyString(), any(Duration.class));
                verify(setOperations).add(eq("user_sessions:user@occupify.com"), eq("refresh_token:refresh-token-123"));
        }

        @Test
        void shouldCheckIfSessionIsValid() {
                when(redisTemplate.hasKey("refresh_token:valid-token")).thenReturn(Mono.just(true));
                when(redisTemplate.hasKey("refresh_token:invalid-token")).thenReturn(Mono.just(false));

                StepVerifier.create(sessionService.isValidSession("valid-token"))
                                .expectNext(true)
                                .verifyComplete();

                StepVerifier.create(sessionService.isValidSession("invalid-token"))
                                .expectNext(false)
                                .verifyComplete();

                StepVerifier.create(sessionService.isValidSession(null))
                                .expectNext(false)
                                .verifyComplete();
        }

        @Test
        void shouldRevokeSingleSession() {
                UUID userId = UUID.randomUUID();
                String json = "{\"userId\":\"" + userId + "\",\"email\":\"user@occupify.com\",\"createdAt\":1000}";
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(redisTemplate.opsForSet()).thenReturn(setOperations);
                when(valueOperations.get("refresh_token:token-to-revoke")).thenReturn(Mono.just(json));
                when(setOperations.remove(eq("user_sessions:user@occupify.com"), eq("refresh_token:token-to-revoke")))
                                .thenReturn(Mono.just(1L));
                when(redisTemplate.delete("refresh_token:token-to-revoke")).thenReturn(Mono.just(1L));

                StepVerifier.create(sessionService.revokeSession("token-to-revoke"))
                                .verifyComplete();

                verify(redisTemplate).delete("refresh_token:token-to-revoke");
        }

        @Test
        void shouldRevokeAllUserSessionsViaLuaScript() {
                when(redisTemplate.execute(eq(revokeAllSessionsScript), anyList()))
                                .thenReturn(Flux.just(3L));

                StepVerifier.create(sessionService.revokeAllUserSessions("user@occupify.com"))
                                .expectNext(3L)
                                .verifyComplete();

                verify(redisTemplate).execute(eq(revokeAllSessionsScript),
                                eq(List.of("user_sessions:user@occupify.com")));
        }
}
