package org.vaadin.tatu.vaadincreate.backend.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LockingEvent.class, name = "LockingEvent"),
        @JsonSubTypes.Type(value = MessageEvent.class, name = "MessageEvent"),
        @JsonSubTypes.Type(value = BooksChangedEvent.class, name = "BooksChangedEvent"),
        @JsonSubTypes.Type(value = CategoriesUpdatedEvent.class, name = "CategoriesUpdatedEvent"),
        @JsonSubTypes.Type(value = UserUpdatedEvent.class, name = "UserUpdatedEvent"),
        @JsonSubTypes.Type(value = ShutdownEvent.class, name = "ShutdownEvent") })
public sealed interface AbstractEvent
        permits LockingEvent, MessageEvent, BooksChangedEvent,
        CategoriesUpdatedEvent, UserUpdatedEvent, ShutdownEvent {
    // This interface serves as a marker for all event types in the system.
    // It can be extended with common methods or properties if needed in the
    // future.
}
