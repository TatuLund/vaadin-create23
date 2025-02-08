package org.vaadin.tatu.vaadincreate.eventbus;

import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Super simple event bus to be used with CDI, e.g. as application scoped
 * broadcaster.
 */
@NullMarked
@SuppressWarnings("java:S6548")
public class EventBusImpl implements EventBus {

    private static EventBusImpl instance;

    /**
     * It is <em>VERY IMPORTANT</em> we use a weak hash map when registering
     * Vaadin components. Without it, this class would keep references to the UI
     * objects forever, causing a massive memory leak.
     */
    private final WeakHashMap<EventBusListener, Object> eventListeners = new WeakHashMap<>();

    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    public static synchronized EventBus getInstance() {
        if (instance == null) {
            instance = new EventBusImpl();
        }
        return instance;
    }

    private EventBusImpl() {
    }

    @Override
    public void post(Object event) {
        synchronized (eventListeners) {
            logger.debug("EventBus event fired for {} recipients.",
                    eventListeners.size());
            eventListeners.forEach((listener, o) -> executor
                    .execute(() -> listener.eventFired(event)));
        }
    }

    @Override
    public void registerEventBusListener(EventBusListener listener) {
        synchronized (eventListeners) {
            logger.debug("EventBus listenerer ({}) registered",
                    listener.hashCode());
            if (eventListeners.put(listener, null) != null) {
                logger.warn("EventBus listener ({}) was already registered",
                        listener.hashCode());
            }
        }
    }

    @Override
    public void unregisterEventBusListener(EventBusListener listener) {
        synchronized (eventListeners) {
            if (eventListeners.remove(listener) != null) {
                logger.debug("EventBus listenerer ({}) un-registered",
                        listener.hashCode());
            }
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
