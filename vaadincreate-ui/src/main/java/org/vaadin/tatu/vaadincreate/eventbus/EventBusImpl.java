package org.vaadin.tatu.vaadincreate.eventbus;

import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Super simple event bus to be used with CDI, e.g. as application scoped
 * broadcaster.
 */
@SuppressWarnings("java:S6548")
public class EventBusImpl implements EventBus {

    private static EventBusImpl instance;

    /**
     * It is <em>VERY IMPORTANT</em> we use a weak hash map when registering
     * Vaadin components. Without it, this class would keep references to the UI
     * objects forever, causing a massive memory leak.
     */
    private final WeakHashMap<EventBusListener, Object> eventListeners = new WeakHashMap<>();

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
            eventListeners.forEach((listener, o) -> listener.eventFired(event));
        }
    }

    @Override
    public void registerEventBusListener(EventBusListener listener) {
        synchronized (eventListeners) {
            logger.debug("EventBus listenerer ({}) registered",
                    listener.hashCode());
            eventListeners.put(listener, null);
        }
    }

    @Override
    public void unregisterEventBusListener(EventBusListener listener) {
        synchronized (eventListeners) {
            logger.debug("EventBus listenerer ({}) un-registered",
                    listener.hashCode());
            eventListeners.remove(listener);
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
