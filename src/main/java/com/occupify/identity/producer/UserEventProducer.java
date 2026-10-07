package com.occupify.identity.producer;

import com.occupify.identity.event.PasswordResetRequestedEvent;
import com.occupify.identity.event.UserRegisteredEvent;
import reactor.core.publisher.Mono;

public interface UserEventProducer {

    Mono<Void> publishUserRegistered(UserRegisteredEvent event);

    Mono<Void> publishPasswordResetRequested(PasswordResetRequestedEvent event);
}
