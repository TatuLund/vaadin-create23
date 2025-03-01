package org.vaadin.tatu.vaadincreate.backend;

import java.util.function.Consumer;

import org.vaadin.tatu.vaadincreate.backend.service.RedisPubSubServiceImpl;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

public interface RedisPubSubService {

    public void publishEvent(String nodeId, Object event);

    public void startSubscriber(Consumer<EventEnvelope> envelopeHandler);

    public void stopSubscriber();

    public void closePublisher();

    public static RedisPubSubService get() {
        return RedisPubSubServiceImpl.getInstance();
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public record EventEnvelope(String nodeId, Object event) {
    }
}