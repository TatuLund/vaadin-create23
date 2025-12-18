package org.vaadin.tatu.vaadincreate.backend.data;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
@Entity
public class Category extends AbstractEntity {

    @NotNull(message = "{category.required}")
    @Size(min = 5, max = 40, message = "{category.length}")
    @Column(name = "category_name")
    @Nullable
    private String name;

    public Category() {
    }

    /**
     * Copy constructor for creating a new Category instance by copying the
     * properties of an existing Category.
     *
     * @param category
     *            the Category instance to copy from
     */
    public Category(Category category) {
        Objects.requireNonNull(category, "category must not be null");
        id = category.id;
        name = category.name;
        version = category.version;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
