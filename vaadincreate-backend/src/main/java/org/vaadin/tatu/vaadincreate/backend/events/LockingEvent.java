package org.vaadin.tatu.vaadincreate.backend.events;

/**
 * Locking event, which will be fired when object is locked or unlocked.
 */
public record LockingEvent(Class<?> type, Integer id, Integer userId,
                String userName, boolean locked) implements AbstractEvent {
}
