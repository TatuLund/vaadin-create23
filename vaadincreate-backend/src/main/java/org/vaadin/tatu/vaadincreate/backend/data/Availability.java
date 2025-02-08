package org.vaadin.tatu.vaadincreate.backend.data;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum Availability {
    COMING("Coming"), AVAILABLE("Available"), DISCONTINUED("Discontinued");

    private final String name;

    private Availability(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
