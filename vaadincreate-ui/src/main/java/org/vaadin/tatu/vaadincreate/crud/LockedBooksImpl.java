package org.vaadin.tatu.vaadincreate.crud;

import java.util.Collection;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;

@SuppressWarnings("serial")
public class LockedBooksImpl implements LockedBooks {

    private static LockedBooksImpl INSTANCE;
    private EventBus eventBus = EventBus.get();

    private final WeakHashMap<Integer, Object> books = new WeakHashMap<>();

    public synchronized static LockedBooks getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LockedBooksImpl();
        }
        return INSTANCE;
    }

    private LockedBooksImpl() {
    }

    @Override
    public Collection<Integer> lockedBooks() {
        synchronized (books) {
            return books.keySet();
        }
    }

    @Override
    public void lock(Integer id) {
        synchronized (books) {
            var match = books.keySet().stream().filter(i -> i.equals(id))
                    .findFirst();
            if (match.isPresent()) {
                throw new IllegalStateException(
                        "Can't open book already opened: " + id);
            }
            books.put(id, null);
            eventBus.post(new BookEvent(id));
            logger.info("Locked book {}", id);
        }
    }

    @Override
    public void unlock(Integer id) {
        synchronized (books) {
            var match = books.keySet().stream().filter(i -> i.equals(id))
                    .findFirst();
            if (match.isPresent()) {
                books.remove(match.get());
            }
            eventBus.post(new BookEvent(id));
            logger.info("Unlocked book {}", id);
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
