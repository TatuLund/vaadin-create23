package org.vaadin.tatu.vaadincreate.crud;

import java.io.Serializable;
import java.util.Collection;

@SuppressWarnings("serial")
public interface LockedBooks {

    public Collection<Integer> lockedBooks();
    
    public void lock(Integer id);

    public void unlock(Integer id);

    public static LockedBooks get() {
        return LockedBooksImpl.getInstance();
    }

    public static class BookEvent implements Serializable {
        private Integer id;

        public BookEvent(Integer id) {
            this.id = id;
        }

        public Integer getId() {
            return id;
        }
    }
}
