package com.occupify.identity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.occupify.identity.event.ContractAcceptedEvent;
import com.occupify.identity.event.ContractCompensationRequestedEvent;
import com.occupify.identity.event.ContractCreatedEvent;
import com.occupify.identity.event.ContractRejectedEvent;
import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange:occupify.notification.exchange}")
    private String exchangeName = "occupify.notification.exchange";

    @Value("${app.rabbitmq.user-exchange:occupify.user.exchange}")
    private String userExchangeName = "occupify.user.exchange";

    @Value("${app.rabbitmq.contract-exchange:occupify.contract.exchange}")
    private String contractExchangeName = "occupify.contract.exchange";

    @Value("${app.rabbitmq.contract-queue:occupify.identity.contract.queue}")
    private String contractQueueName = "occupify.identity.contract.queue";

    @Value("${app.rabbitmq.contract-accepted-routing-key:contract.accepted}")
    private String contractAcceptedRoutingKey = "contract.accepted";

    @Value("${app.rabbitmq.contract-rejected-routing-key:contract.rejected}")
    private String contractRejectedRoutingKey = "contract.rejected";

    @Value("${app.rabbitmq.contract-created-routing-key:contract.created}")
    private String contractCreatedRoutingKey = "contract.created";

    @Value("${app.rabbitmq.contract-compensation-routing-key:contract.compensation.#}")
    private String contractCompensationRoutingKey = "contract.compensation.#";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(userExchangeName, true, false);
    }

    @Bean
    public TopicExchange contractExchange() {
        return new TopicExchange(contractExchangeName, true, false);
    }

    @Bean
    public Queue identityContractQueue() {
        return QueueBuilder.durable(contractQueueName).build();
    }

    @Bean
    public Binding identityContractAcceptedBinding(Queue identityContractQueue, TopicExchange contractExchange) {
        return BindingBuilder.bind(identityContractQueue).to(contractExchange).with(contractAcceptedRoutingKey);
    }

    @Bean
    public Binding identityContractRejectedBinding(Queue identityContractQueue, TopicExchange contractExchange) {
        return BindingBuilder.bind(identityContractQueue).to(contractExchange).with(contractRejectedRoutingKey);
    }

    @Bean
    public Binding identityContractCreatedBinding(Queue identityContractQueue, TopicExchange contractExchange) {
        return BindingBuilder.bind(identityContractQueue).to(contractExchange).with(contractCreatedRoutingKey);
    }

    @Bean
    public Binding identityContractCompensationBinding(Queue identityContractQueue, TopicExchange contractExchange) {
        return BindingBuilder.bind(identityContractQueue).to(contractExchange).with(contractCompensationRoutingKey);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("*");

        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("com.occupify.core.event.ContractAcceptedEvent", ContractAcceptedEvent.class);
        idClassMapping.put("com.occupify.identity.event.ContractAcceptedEvent", ContractAcceptedEvent.class);
        idClassMapping.put("ContractAcceptedEvent", ContractAcceptedEvent.class);

        idClassMapping.put("com.occupify.core.event.ContractRejectedEvent", ContractRejectedEvent.class);
        idClassMapping.put("com.occupify.identity.event.ContractRejectedEvent", ContractRejectedEvent.class);
        idClassMapping.put("ContractRejectedEvent", ContractRejectedEvent.class);

        idClassMapping.put("com.occupify.core.event.ContractCreatedEvent", ContractCreatedEvent.class);
        idClassMapping.put("com.occupify.identity.event.ContractCreatedEvent", ContractCreatedEvent.class);
        idClassMapping.put("ContractCreatedEvent", ContractCreatedEvent.class);

        idClassMapping.put("com.occupify.core.event.ContractCompensationRequestedEvent", ContractCompensationRequestedEvent.class);
        idClassMapping.put("com.occupify.identity.event.ContractCompensationRequestedEvent", ContractCompensationRequestedEvent.class);
        idClassMapping.put("ContractCompensationRequestedEvent", ContractCompensationRequestedEvent.class);

        typeMapper.setIdClassMapping(idClassMapping);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
