package org.vaadin.tatu.vaadincreate.backend.events;

/**
 * Represents a change event for books. This record encapsulates the details of
 * a change made to a book, including the product affected and the type of
 * change.
 */
public record BooksChangedEvent(Integer productId,
        BookChange change) implements AbstractEvent {
    public enum BookChange {
        SAVE, DELETE
    }
}
