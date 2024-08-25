package org.vaadin.tatu.vaadincreate.locking;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
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

    private final WeakHashMap<LockedObject, Object> lockedObjects = new WeakHashMap<>();

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
        return isLocked(object.getClass(), object.getId());
    }

    public User isLocked(Class<?> type, Integer id) {
        synchronized (lockedObjects) {
            var match = findObject(type, id);
            if (match.isPresent()) {
                return match.get().user;
            }
        }
        return null;
    }

    @Override
    public void lock(AbstractEntity object, User user) {
        Objects.requireNonNull(object, NOT_NULL_ERROR);
        Objects.requireNonNull(user, "user can't be null");
        lock(object.getClass(), object.getId(), user);
    }

    private void lock(Class<?> type, Integer id, User user) {
        if (id != null && id < 0) {
            throw new IllegalArgumentException(
                    "Id can't be null and must be positive");
        }
        synchronized (lockedObjects) {
            var match = findObject(type, id);
            if (match.isPresent()) {
                throw new IllegalStateException(
                        "Can't lock object already locked: " + id);
            }
            lockedObjects.put(new LockedObject(type, id, user), null);
            eventBus.post(new LockingEvent(type, id, user, true));
            logger.debug("{} locked {} ({})", user.getName(),
                    type.getSimpleName(), id);
        }
    }

    @Override
    public void unlock(AbstractEntity object) {
        Objects.requireNonNull(object, NOT_NULL_ERROR);
        unlock(object.getClass(), object.getId());
    }

    private void unlock(Class<?> type, Integer id) {
        synchronized (lockedObjects) {
            var match = findObject(type, id);
            if (match.isPresent()) {
                var object = match.get();
                lockedObjects.remove(object);
                eventBus.post(new LockingEvent(type, id, object.user, false));
                logger.debug("{} unlocked {} ({})", object.user.getName(),
                        type.getSimpleName(), id);
            }
        }
    }

    private Optional<LockedObject> findObject(Class<?> type, Integer id) {
        return lockedObjects.keySet().stream()
                .filter(obj -> obj.type.equals(type) && obj.id.equals(id))
                .findFirst();
    }

    static class LockedObject implements Serializable {
        Integer id;
        Class<?> type;
        User user;

        LockedObject(Class<?> type, Integer id, User user) {
            this.type = type;
            this.id = id;
            this.user = user;
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
