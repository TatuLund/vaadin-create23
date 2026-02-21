package org.vaadin.tatu.vaadincreate.backend.events;

/**
 * Represents an event that is published when a purchase status changes (e.g.
 * approved or rejected). Carries only the purchase id; consumers must fetch the
 * latest {@code Purchase} from the service.
 */
public record PurchaseStatusChangedEvent(Integer purchaseId)
        implements
            AbstractEvent {
}
