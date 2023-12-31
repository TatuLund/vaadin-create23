package org.vaadin.tatu.vaadincreate.eventbus;

import java.io.Serializable;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Super simple event bus to be used with CDI, e.g. as application scoped
 * broadcaster.
 */
@SuppressWarnings("serial")
public class EventBusImpl implements EventBus, Serializable {

    private static EventBusImpl INSTANCE;

    /**
     * It is <em>VERY IMPORTANT</em> we use a weak hash map when registering
     * Vaadin components. Without it, this class would keep references to the UI
     * objects forever, causing a massive memory leak.
     */
    private final WeakHashMap<EventBusListener, Object> eventListeners = new WeakHashMap<>();

    public synchronized static EventBus getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new EventBusImpl();
        }
        return INSTANCE;
    }

    public EventBusImpl() {
    }

    public void post(Object event) {
        synchronized (eventListeners) {
            logger.info("EventBus ({}) event fired for {} recipients.",
                    this.hashCode(), eventListeners.size());
            eventListeners.forEach((listener, o) -> {
                listener.eventFired(event);
            });
        }
    }

    public void registerEventBusListener(EventBusListener listener) {
        synchronized (eventListeners) {
            logger.info("EventBus ({}) listenerer registered", this.hashCode());
            eventListeners.put(listener, null);
        }
    }

    public void unregisterEventBusListener(EventBusListener listener) {
        synchronized (eventListeners) {
            logger.info("EventBus ({}) listenerer un-registered",
                    this.hashCode());
            eventListeners.remove(listener);
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
