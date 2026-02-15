package org.vaadin.tatu.vaadincreate.backend.events;

public record CategoriesUpdatedEvent(Integer categoryId,
        CategoryChange change) implements AbstractEvent {
    public enum CategoryChange {
        SAVE, DELETE
    }
}