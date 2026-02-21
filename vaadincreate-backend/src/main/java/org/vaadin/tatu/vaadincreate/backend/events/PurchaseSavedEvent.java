package org.vaadin.tatu.vaadincreate.backend.events;

/**
 * Represents an event that is published when a new purchase is saved (created).
 * Carries only the purchase id; consumers must fetch the latest
 * {@code Purchase} from the service.
 */
public record PurchaseSavedEvent(Integer purchaseId) implements AbstractEvent {
}
