package com.occupify.identity.producer.impl;

import com.occupify.identity.event.PasswordResetRequestedEvent;
import com.occupify.identity.event.UserActivatedEvent;
import com.occupify.identity.event.UserRegisteredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserEventProducerImplTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private UserEventProducerImpl userEventProducer;

    @BeforeEach
    void setUp() {
        userEventProducer = new UserEventProducerImpl(rabbitTemplate);
        ReflectionTestUtils.setField(userEventProducer, "exchange", "occupify.notification.exchange");
        ReflectionTestUtils.setField(userEventProducer, "userExchange", "occupify.user.exchange");
        ReflectionTestUtils.setField(userEventProducer, "userRegisteredRoutingKey", "user.registered");
        ReflectionTestUtils.setField(userEventProducer, "userActivatedRoutingKey", "user.activated");
        ReflectionTestUtils.setField(userEventProducer, "passwordResetRoutingKey", "user.password-reset");
    }

    @Test
    void shouldPublishUserRegisteredEvent() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = new UserRegisteredEvent(userId, "test@occupify.com", "123456", Instant.now());

        userEventProducer.publishUserRegistered(event);

        ArgumentCaptor<UserRegisteredEvent> captor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("occupify.notification.exchange"), eq("user.registered"), captor.capture());

        UserRegisteredEvent captured = captor.getValue();
        assertEquals(userId, captured.userId());
        assertEquals("test@occupify.com", captured.email());
        assertEquals("123456", captured.otpCode());
    }

    @Test
    void shouldPublishUserActivatedEvent() {
        UUID userId = UUID.randomUUID();
        UserActivatedEvent event = new UserActivatedEvent(userId, "test@occupify.com", null, "USER", "ACTIVE", Instant.now());

        userEventProducer.publishUserActivated(event);

        ArgumentCaptor<UserActivatedEvent> captor = ArgumentCaptor.forClass(UserActivatedEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("occupify.user.exchange"), eq("user.activated"), captor.capture());

        UserActivatedEvent captured = captor.getValue();
        assertEquals(userId, captured.userId());
        assertEquals("test@occupify.com", captured.email());
        assertEquals("USER", captured.role());
        assertEquals("ACTIVE", captured.status());
    }

    @Test
    void shouldPublishPasswordResetRequestedEvent() {
        UUID userId = UUID.randomUUID();
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(userId, "test@occupify.com", "654321", Instant.now());

        userEventProducer.publishPasswordResetRequested(event);

        ArgumentCaptor<PasswordResetRequestedEvent> captor = ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("occupify.notification.exchange"), eq("user.password-reset"), captor.capture());

        PasswordResetRequestedEvent captured = captor.getValue();
        assertEquals(userId, captured.userId());
        assertEquals("test@occupify.com", captured.email());
        assertEquals("654321", captured.otpCode());
    }
}
