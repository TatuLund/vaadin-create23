package org.vaadin.tatu.vaadincreate.backend.data;

import java.io.Serializable;
import java.util.Objects;

import javax.validation.constraints.NotNull;

@SuppressWarnings("serial")
public class Category implements Serializable {

    @NotNull
    private int id = -1;
    @NotNull
    private String name;

    public Category() {
    }

    public Category(Category category) {
        id = category.id;
        name = category.name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Category other = (Category) obj;
        return id == other.id;
    }

    @Override
    public String toString() {
        return getName();
    }
}
