package com.occupify.identity.producer;

import com.occupify.identity.event.PasswordResetRequestedEvent;
import com.occupify.identity.event.UserActivatedEvent;
import com.occupify.identity.event.UserRegisteredEvent;

public interface UserEventProducer {

    void publishUserRegistered(UserRegisteredEvent event);

    void publishUserActivated(UserActivatedEvent event);

    void publishPasswordResetRequested(PasswordResetRequestedEvent event);
}
