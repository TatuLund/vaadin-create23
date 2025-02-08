package org.vaadin.tatu.vaadincreate.eventbus;

import org.jspecify.annotations.NullMarked;

/**
 * The EventBus interface provides a mechanism for posting events and
 * registering listeners that will be notified when events are fired. It follows
 * the singleton pattern to ensure that there is only one instance of the
 * EventBus.
 *
 * <p>
 * Listeners can be registered to receive events, and they can also be
 * unregistered when they no longer need to receive events. Events are posted to
 * the event bus, and all registered listeners will be notified of the event.
 * </p>
 *
 * <p>
 * Usage example:
 * </p>
 *
 * <pre>
 * {@code
 * EventBus eventBus = EventBus.get();
 * eventBus.registerEventBusListener(event -> {
 *     // Handle the event
 * });
 * eventBus.post(new SomeEvent());
 * }
 * </pre>
 *
 * <p>
 * Note: The event object passed to the {@code post} method should not be null.
 * </p>
 */
@NullMarked
public interface EventBus {

    /**
     * The EventBusListener interface should be implemented by any class that
     * wishes to receive events from the EventBus. The implementing class must
     * define the behavior for handling the event in the eventFired method.
     */
    public interface EventBusListener {
        /**
         * This method is called when an event is fired on the event bus.
         *
         * @param event
         *            the event object that was fired
         */
        public void eventFired(Object event);
    }

    /**
     * Posts an event to the event bus. All registered subscribers will be
     * notified of the event.
     *
     * @param event
     *            the event object to be posted. It should not be null.
     */
    public void post(Object event);

    /**
     * Registers an event bus listener to receive events.
     *
     * @param listener
     *            the listener to be registered
     */
    public void registerEventBusListener(EventBusListener listener);

    /**
     * Unregisters the specified listener from the event bus.
     *
     * @param listener
     *            the listener to be unregistered
     */
    public void unregisterEventBusListener(EventBusListener listener);

    /**
     * Retrieves the singleton instance of the EventBus.
     *
     * @return the singleton instance of EventBus.
     */
    public static EventBus get() {
        return EventBusImpl.getInstance();
    }

}
