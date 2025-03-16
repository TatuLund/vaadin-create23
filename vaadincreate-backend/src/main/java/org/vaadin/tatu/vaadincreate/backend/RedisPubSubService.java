package org.vaadin.tatu.vaadincreate.backend;

import java.util.function.Consumer;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.backend.events.AbstractEvent;
import org.vaadin.tatu.vaadincreate.backend.service.RedisPubSubServiceImpl;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@NullMarked
public interface RedisPubSubService {

    /**
     * Publishes an event wrapped in an envelope with metadata.
     *
     * @param nodeId
     *            the identifier for the node sending the event, not null.
     * @param event
     *            the event object to publish, not null.
     */
    public void publishEvent(String nodeId, AbstractEvent event);

    /**
     * Starts the subscriber which will deliver a deserialized EventEnvelope via
     * the provided callback.
     *
     * @param envelopeHandler
     *            a callback to handle each incoming envelope, not null.
     */
    public void startSubscriber(Consumer<EventEnvelope> envelopeHandler);

    /**
     * Stops the subscriber.
     */
    public void stopSubscriber();

    /**
     * Closes the publisher.
     */
    public void closePublisher();

    /**
     * Returns the singleton instance of RedisPubSubService.
     *
     * @return the singleton instance, not null.
     */
    public static RedisPubSubService get() {
        return RedisPubSubServiceImpl.getInstance();
    }

    /**
     * Represents an envelope for events, containing metadata such as the node
     * ID and the event object itself.
     *
     * @param nodeId
     *            the identifier for the node sending the event, not null.
     * @param event
     *            the event object, not null.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public record EventEnvelope(String nodeId, AbstractEvent event) {
    }
}