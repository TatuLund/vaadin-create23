package org.vaadin.tatu.vaadincreate.backend.data;

import java.io.Serializable;
import java.util.Objects;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.persistence.OptimisticLockException;

@SuppressWarnings("serial")
public abstract class AbstractEntity implements Serializable {

    @NotNull
    @Min(0)
    int id = -1;

    int version = 0;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        if (version - this.version == 1) {
            this.version = version;
        } else {
            throw new OptimisticLockException(this);
        }
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
        AbstractEntity other = (AbstractEntity) obj;
        return id == other.id;
    }
}
