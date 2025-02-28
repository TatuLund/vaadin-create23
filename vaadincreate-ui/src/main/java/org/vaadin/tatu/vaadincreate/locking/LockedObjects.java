package org.vaadin.tatu.vaadincreate.locking;

import java.io.Serializable;

import javax.annotation.Nullable;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.backend.data.AbstractEntity;
import org.vaadin.tatu.vaadincreate.backend.data.User;

/**
 * This is a ledger for locked objects used for pessimistic locking.
 */
@NullMarked
public interface LockedObjects {

    /**
     * Check which user has locked object.
     *
     * @param object
     *            AnstractEntity
     * @return The user who has locked the object, null if the object is not
     *         locked.
     */
    @Nullable
    public User isLocked(AbstractEntity object);

    /**
     * Lock object for the given user
     *
     * @param object
     *            AbstractEntity
     * @param user
     *            User holding the lock
     */
    public void lock(AbstractEntity object, User user);

    /**
     * Unlock the object
     *
     * @param object
     *            AbstractEntity
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
    public record LockingEvent(Class<?> type, Integer id, Integer userId, boolean locked) implements Serializable {
    }
}
