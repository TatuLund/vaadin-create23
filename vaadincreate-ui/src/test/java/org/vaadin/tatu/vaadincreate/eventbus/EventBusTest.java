package org.vaadin.tatu.vaadincreate.eventbus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.backend.RedisPubSubService;
import org.vaadin.tatu.vaadincreate.backend.RedisPubSubService.EventEnvelope;
import org.vaadin.tatu.vaadincreate.backend.events.AbstractEvent;
import org.vaadin.tatu.vaadincreate.backend.events.MessageEvent;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;

public class EventBusTest {

    private static RedisPubSubService redisService = new MockPubSubService();
    private static EventBusImpl eventBus = new EventBusImpl(redisService);
    private static Consumer<EventEnvelope> envelopeHandler;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private WeakHashMap<EventBusListener, Object> listenersBackup;
    private static CountDownLatch latch = new CountDownLatch(1);

    @Before
    public void setStreams() {
        listenersBackup = eventBus.eventListeners;
        eventBus.eventListeners = new WeakHashMap<>();
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
    }

    @After
    public void restoreInitialStreams() {
        var log = out.toString();
        System.setOut(originalOut);
        System.setErr(originalErr);

        System.out.println(log);
        eventBus.eventListeners = listenersBackup;
    }

    @Test
    @SuppressWarnings({ "unused", "java:S1854", "java:S2925" })
    public void eventFiredAndRemoval() {
        var listener1 = new TestListener();
        var listener2 = new TestListener();
        var listener3 = new TestListener();
        var event = new MessageEvent("Hello", LocalDateTime.now());
        eventBus.post(event);
        // Wait for latch
        try {
            latch.await();
            wait10ms(); // Wait loggers to print
        } catch (InterruptedException e) {
            // Ignore
        }

        assertEquals(1, listener1.getEventCount());
        assertEquals("Hello", listener1.getLastEvent().message());
        assertEquals(1, listener2.getEventCount());
        assertEquals("Hello", listener2.getLastEvent().message());
        assertEquals(1, listener3.getEventCount());
        assertEquals("Hello", listener3.getLastEvent().message());

        listener1.remove();
        listener3 = null;

        System.gc();
        wait100ms(); // Wait for GC to run

        event = new MessageEvent("World", LocalDateTime.now());
        latch = new CountDownLatch(1);

        eventBus.post(event);
        // Wait for latch
        try {
            latch.await();
            wait10ms(); // Wait loggers to print
        } catch (InterruptedException e) {
            // Ignore
        }

        assertEquals(1, listener1.getEventCount());
        assertEquals(2, listener2.getEventCount());
        assertEquals("World", listener2.getLastEvent().message());
        assertTrue(out.toString().contains("event fired for 1 recipients."));

        listener2.remove();
    }

    @Test
    public void messageReceivedFromRedisIsRelayedLocally() {
        var listener = new TestListener();
        triggerRedisEvent();

        try {
            latch.await();
            wait10ms(); // Wait loggers to print
        } catch (InterruptedException e) {
            // Ignore
        }

        assertEquals(1, listener.getEventCount());
        assertEquals("Redis", listener.getLastEvent().message());

        listener.remove();
    }

    public void triggerRedisEvent() {
        var event = new MessageEvent("Redis", LocalDateTime.now());
        envelopeHandler
                .accept(new EventEnvelope(UUID.randomUUID().toString(), event));
    }

    private void wait100ms() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    private void wait10ms() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    public static class TestListener implements EventBusListener {

        private AtomicInteger count = new AtomicInteger(0);
        private MessageEvent event;

        public TestListener() {
            eventBus.registerEventBusListener(this);
        }

        @Override
        public void eventFired(AbstractEvent event) {
            count.incrementAndGet();
            if (event instanceof MessageEvent) {
                this.event = (MessageEvent) event;
            }
            latch.countDown();
        }

        public int getEventCount() {
            return count.get();
        }

        public MessageEvent getLastEvent() {
            return event;
        }

        public void remove() {
            eventBus.unregisterEventBusListener(this);
        }
    }

    public static class MockPubSubService implements RedisPubSubService {

        @Override
        public void publishEvent(String nodeId, AbstractEvent event) {
            // No-op
        }

        @Override
        public void startSubscriber(Consumer<EventEnvelope> handler) {
            envelopeHandler = handler;
        }

        @Override
        public void stopSubscriber() {
            // No-op
        }

        @Override
        public void closePublisher() {
            // No-op
        }

    }
}
