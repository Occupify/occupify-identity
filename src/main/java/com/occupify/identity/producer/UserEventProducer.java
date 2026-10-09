package com.occupify.identity.producer;

import com.occupify.identity.event.PasswordResetRequestedEvent;
import com.occupify.identity.event.UserRegisteredEvent;

public interface UserEventProducer {

    void publishUserRegistered(UserRegisteredEvent event);

    void publishPasswordResetRequested(PasswordResetRequestedEvent event);
}
