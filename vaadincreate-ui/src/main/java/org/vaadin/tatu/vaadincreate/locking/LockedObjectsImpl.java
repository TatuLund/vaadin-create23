package org.vaadin.tatu.vaadincreate.locking;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;

@SuppressWarnings("serial")
public class LockedObjectsImpl implements LockedObjects {

    private static LockedObjectsImpl INSTANCE;
    private EventBus eventBus = EventBus.get();

    private final WeakHashMap<LockedObject, Object> lockedObjects = new WeakHashMap<>();

    public synchronized static LockedObjects getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LockedObjectsImpl();
        }
        return INSTANCE;
    }

    private LockedObjectsImpl() {
    }

    @Override
    public User isLocked(Class<?> type, Integer id) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(id);
        synchronized (lockedObjects) {
            var match = findObject(type, id);
            if (match.isPresent()) {
                return match.get().user;
            }
        }
        return null;
    }

    @Override
    public void lock(Class<?> type, Integer id, User user) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(user);
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
    public void unlock(Class<?> type, Integer id) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(id);
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
