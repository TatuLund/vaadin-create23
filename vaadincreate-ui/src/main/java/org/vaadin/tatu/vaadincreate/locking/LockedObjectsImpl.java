package org.vaadin.tatu.vaadincreate.locking;

import java.util.Objects;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.data.AbstractEntity;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;

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
        Objects.requireNonNull(user, "user can't be null");
        synchronized (lockedObjects) {
            if (lockedObjects.containsKey(object)) {
                throw new IllegalStateException(
                        "Can't lock object already locked: " + object.getId());
            }
            lockedObjects.put(object, user);
            eventBus.post(new LockingEvent(object.getClass(), object.getId(), user, true));
            logger.debug("{} locked {} ({})", user.getName(),
                    object.getClass().getSimpleName(), object.getId());
        }
    }

    @Override
    public void unlock(AbstractEntity object) {
        Objects.requireNonNull(object, NOT_NULL_ERROR);
        synchronized (lockedObjects) {
            User user = lockedObjects.remove(object);
            if (user != null) {
                eventBus.post(new LockingEvent(object.getClass(), object.getId(), user, false));
                logger.debug("{} unlocked {} ({})", user.getName(),
                        object.getClass().getSimpleName(), object.getId());
            }
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}