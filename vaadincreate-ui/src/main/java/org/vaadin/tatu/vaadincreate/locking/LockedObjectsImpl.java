package org.vaadin.tatu.vaadincreate.locking;

import java.util.Objects;
import java.util.WeakHashMap;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.data.AbstractEntity;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;

@NullMarked
@SuppressWarnings("java:S6548")
public class LockedObjectsImpl implements LockedObjects {

    private static LockedObjectsImpl instance;
    private EventBus eventBus = EventBus.get();
    private static final String NOT_NULL_ERROR = "object can't be null";

    private final WeakHashMap<AbstractEntity, User> lockedObjects = new WeakHashMap<>();

    public static synchronized LockedObjects getInstance() {
        if (instance == null) {
            instance = new LockedObjectsImpl();
        }
        return instance;
    }

    private LockedObjectsImpl() {
    }

    @Nullable
    @Override
    public User isLocked(AbstractEntity object) {
        Objects.requireNonNull(object, NOT_NULL_ERROR);
        synchronized (lockedObjects) {
            return lockedObjects.get(object);
        }
    }

    @Override
    public void lock(AbstractEntity object, User user) {
        Objects.requireNonNull(object, NOT_NULL_ERROR);
        var id = object.getId();
        Objects.requireNonNull(id, "Can't unlock object with null id");
        Objects.requireNonNull(user, "user can't be null");
        var userId = user.getId();
        Objects.requireNonNull(userId, "user id can't be null");
        synchronized (lockedObjects) {
            if (lockedObjects.containsKey(object)) {
                throw new IllegalStateException(
                        String.format("Can't lock object already locked: %s",
                                object.getId()));
            }
            lockedObjects.put(object, user);
            eventBus.post(
                    new LockingEvent(object.getClass(), id, userId, true));
            logger.debug("{} locked {} ({})", user.getName(),
                    object.getClass().getSimpleName(), object.getId());
        }
    }

    @Override
    public void unlock(AbstractEntity object) {
        Objects.requireNonNull(object, NOT_NULL_ERROR);
        var id = object.getId();
        Objects.requireNonNull(id, "Can't unlock object with null id");
        synchronized (lockedObjects) {
            var user = lockedObjects.remove(object);
            if (user != null) {
                var userId = user.getId();
                assert userId != null : "user id can't be null";
                eventBus.post(
                        new LockingEvent(object.getClass(), id, userId, false));
                logger.debug("{} unlocked {} ({})", user.getName(),
                        object.getClass().getSimpleName(), object.getId());
            }
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}