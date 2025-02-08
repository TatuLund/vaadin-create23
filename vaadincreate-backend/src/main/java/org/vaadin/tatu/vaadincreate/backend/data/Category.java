package org.vaadin.tatu.vaadincreate.backend.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
@Entity
public class Category extends AbstractEntity {

    @NotNull(message = "{category.required}")
    @Size(min = 5, max = 40, message = "{category.length}")
    @Column(name = "category_name")
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
