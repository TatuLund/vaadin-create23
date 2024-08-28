package org.vaadin.tatu.vaadincreate.backend.data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@SuppressWarnings("serial")
public class Category extends AbstractEntity {

    @NotNull(message = "{category.required}")
    @Size(min = 5, max = 40, message = "{category.length}")
    private String name;

    public Category() {
    }

    public Category(Category category) {
        id = category.id;
        name = category.name;
        version = category.version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return getName();
    }
}
