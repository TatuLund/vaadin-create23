package org.vaadin.tatu.vaadincreate.backend;

import org.jspecify.annotations.NullMarked;

/**
 * Thrown when an entity cannot be deleted because it is referenced by other
 * persisted data (for example purchase history).
 * <p>
 * This is a domain-level exception intended for the UI layer to catch and
 * render as a human-friendly message.
 */
@NullMarked
public class EntityInUseException extends RuntimeException {

    private final String entityType;
    private final String entityId;

    public EntityInUseException(String entityType, String entityId,
            Throwable cause) {
        super(entityType + " " + entityId
                + " cannot be deleted because it is referenced", cause);
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }
}
