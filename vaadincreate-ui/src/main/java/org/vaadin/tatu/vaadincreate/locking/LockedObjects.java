package org.vaadin.tatu.vaadincreate.locking;

import java.io.Serializable;

import org.vaadin.tatu.vaadincreate.backend.data.AbstractEntity;
import org.vaadin.tatu.vaadincreate.backend.data.User;

/**
 * This is a ledger for locked objects used for pessimistic locking.
 */
public interface LockedObjects {

    /**
     * Check which user has locked object.
     *
     * @param object
     *            AnstractEntity
     * @return The user who has locked the object, null if the object is not
     *         locked.
     */
    public User isLocked(AbstractEntity object);

    /**
     * Lock object for the given user
     *
     * @param object
     *            AnstractEntity
     * @param user
     *            User holding the lock
     */
    public void lock(AbstractEntity object, User user);

    /**
     * Unlock the object
     *
     * @param object
     *            AnstractEntity
     * @param id
     *            The id of the object
     */
    public void unlock(AbstractEntity object);

    public static LockedObjects get() {
        return LockedObjectsImpl.getInstance();
    }

    /**
     * Locking event, which will be fired when object is locked or unlocked.
     */
    public static class LockingEvent implements Serializable {
        private Integer id;
        private User user;
        private Class<?> type;
        private boolean locked;

        public LockingEvent(Class<?> type, Integer id, User user,
                boolean locked) {
            this.id = id;
            this.type = type;
            this.user = user;
            this.locked = locked;
        }

        /**
         * Get user who triggered the locking event.
         *
         * @return The user
         */
        public User getUser() {
            return user;
        }

        /**
         * Get id of the object.
         *
         * @return Integer value
         */
        public Integer getId() {
            return id;
        }

        /**
         * Get the type of the object.
         *
         * @return The type
         */
        public Class<?> getType() {
            return type;
        }

        /**
         * Check if the event was due locking or unlocking of the object.
         *
         * @return boolean value
         */
        public boolean isLocked() {
            return locked;
        }
    }
}
