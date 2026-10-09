package com.occupify.identity.producer.impl;

import com.occupify.identity.event.PasswordResetRequestedEvent;
import com.occupify.identity.event.UserRegisteredEvent;
import com.occupify.identity.producer.UserEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventProducerImpl implements UserEventProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:occupify.notification.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.user-registered-routing-key:user.registered}")
    private String userRegisteredRoutingKey;

    @Value("${app.rabbitmq.password-reset-routing-key:user.password-reset}")
    private String passwordResetRoutingKey;

    @Override
    public void publishUserRegistered(UserRegisteredEvent event) {
        log.info("[Event Producer] Publishing UserRegisteredEvent for user [{}] ({}) to exchange [{}] with routingKey [{}]",
                event.userId(), event.email(), exchange, userRegisteredRoutingKey);
        rabbitTemplate.convertAndSend(exchange, userRegisteredRoutingKey, event);
    }

    @Override
    public void publishPasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("[Event Producer] Publishing PasswordResetRequestedEvent for user [{}] ({}) to exchange [{}] with routingKey [{}]",
                event.userId(), event.email(), exchange, passwordResetRoutingKey);
        rabbitTemplate.convertAndSend(exchange, passwordResetRoutingKey, event);
    }
}
