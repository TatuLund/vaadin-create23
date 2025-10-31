package org.vaadin.tatu.vaadincreate.eventbus;

import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.RedisPubSubService;
import org.vaadin.tatu.vaadincreate.backend.events.AbstractEvent;

/**
 * Super simple event bus to be used with CDI, e.g. as application scoped
 * broadcaster.
 */
@NullMarked
@SuppressWarnings("java:S6548")
public class EventBusImpl implements EventBus {

    private static EventBusImpl instance;
    private final String nodeId = UUID.randomUUID().toString();

    private RedisPubSubService redisService;

    /**
     * It is <em>VERY IMPORTANT</em> we use a weak hash map when registering
     * Vaadin components. Without it, this class would keep references to the UI
     * objects forever, causing a massive memory leak.
     */
    protected WeakHashMap<EventBusListener, Object> eventListeners = new WeakHashMap<>();

    private final ExecutorService executor = Executors.newFixedThreadPool(5,
            Thread.ofVirtual().factory());

    public static synchronized EventBus getInstance() {
        if (instance == null) {
            instance = new EventBusImpl(RedisPubSubService.get());
        }
        return instance;
    }

    protected EventBusImpl(RedisPubSubService redisService) {
        this.redisService = redisService;
        logger.info("Starting EventBus");
        // Start the subscriber and handle incoming envelopes.
        redisService.startSubscriber(envelope -> {
            logger.debug("EventBus event received from {}: {}",
                    envelope.nodeId(), envelope.event());
            // Ignore events from the same node.
            if (!nodeId.equals(envelope.nodeId())) {
                // Dispatch the unwrapped event locally.
                logger.info("Relaying event to local listeners: {}",
                        envelope.event().getClass().getName());
                postLocal(envelope.event());
            }
        });
    }

    @Override
    public void post(AbstractEvent event) {
        // Publish the event using the Redis service.
        redisService.publishEvent(nodeId, event);
        // Immediately dispatch locally.
        postLocal(event);
    }

    private void postLocal(AbstractEvent event) {
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

    @Override
    public void shutdown() {
        logger.info("Shutting down EventBus");
        redisService.stopSubscriber();
        redisService.closePublisher();
        executor.shutdown();
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
