package org.vaadin.tatu.vaadincreate.locking;

import java.io.Serializable;

import org.vaadin.tatu.vaadincreate.backend.data.User;

@SuppressWarnings("serial")
public interface LockedObjects {

    public User isLocked(Class<?> type, Integer id);

    public void lock(Class<?> type, Integer id, User user);

    public void unlock(Class<?> type, Integer id);

    public static LockedObjects get() {
        return LockedObjectsImpl.getInstance();
    }

    public static class LockingEvent implements Serializable {
        private Integer id;
        private User user;
        private Class<?> type;

        public LockingEvent(Class<?> type, Integer id, User user) {
            this.id = id;
            this.type = type;
            this.user = user;
        }

        public User getUser() {
            return user;
        }

        public Integer getId() {
            return id;
        }

        public Class<?> getType() {
            return type;
        }
    }
}
