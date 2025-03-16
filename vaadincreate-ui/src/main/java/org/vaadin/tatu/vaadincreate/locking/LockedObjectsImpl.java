package org.vaadin.tatu.vaadincreate.locking;

import java.io.Serializable;
import java.util.Objects;
import java.util.WeakHashMap;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.UserService;
import org.vaadin.tatu.vaadincreate.backend.data.AbstractEntity;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.events.AbstractEvent;
import org.vaadin.tatu.vaadincreate.backend.events.LockingEvent;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;

@NullMarked
@SuppressWarnings("java:S6548")
public class LockedObjectsImpl implements LockedObjects, EventBusListener {

    private static LockedObjectsImpl instance;
    private EventBus eventBus = EventBus.get();
    private static final String NOT_NULL_ERROR = "object can't be null";

    private final WeakHashMap<Integer, Integer> lockedObjects = new WeakHashMap<>();

    public static synchronized LockedObjects getInstance() {
        if (instance == null) {
            instance = new LockedObjectsImpl();
        }
        return instance;
    }

    private LockedObjectsImpl() {
        eventBus.registerEventBusListener(this);
    }

    @Nullable
    @Override
    public User isLocked(AbstractEntity object) {
        Objects.requireNonNull(object, NOT_NULL_ERROR);
        Integer id = object.getId();
        Objects.requireNonNull(id, NOT_NULL_ERROR);
        Integer userId;
        synchronized (lockedObjects) {
            userId = lockedObjects.get(id);
        }
        if (userId == null) {
            return null;
        }
        UserService userService = UserService.get();
        return userService.getUserById(userId);
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
            if (lockedObjects.containsKey(id)) {
                throw new IllegalStateException(
                        String.format("Can't lock object already locked: %s",
                                object.getId()));
            }
            lockedObjects.put(id, userId);
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
            var userId = lockedObjects.remove(id);
            if (userId != null) {
                eventBus.post(
                        new LockingEvent(object.getClass(), id, userId, false));
                logger.debug("Unlocked {} ({})",
                        object.getClass().getSimpleName(), object.getId());
            }
        }
    }

    @Override
    public void eventFired(AbstractEvent event) {
        if (event instanceof LockingEvent lockingEvent) {
            synchronized (lockedObjects) {
                if (lockingEvent.locked()
                        && !lockedObjects.containsKey(lockingEvent.id())) {
                    logger.debug("Remote locked {} ({}) by user {}",
                            lockingEvent.type().getSimpleName(),
                            lockingEvent.id(), lockingEvent.userId());
                    lockedObjects.put(lockingEvent.id(), lockingEvent.userId());
                } else if (!lockingEvent.locked()
                        && lockedObjects.containsKey(lockingEvent.id())) {
                    logger.debug("Remote unlocked {} ({}) by user {}",
                            lockingEvent.type().getSimpleName(),
                            lockingEvent.id(), lockingEvent.userId());
                    lockedObjects.remove(lockingEvent.id());
                }
            }
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}