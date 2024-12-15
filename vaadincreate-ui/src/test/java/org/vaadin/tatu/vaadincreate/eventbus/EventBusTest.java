package org.vaadin.tatu.vaadincreate.eventbus;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;

public class EventBusTest {

    EventBus eventBus = EventBus.get();

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Before
    public void setStreams() {
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
    }

    @After
    public void restoreInitialStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void eventFiredAndRemoval() {
        var listener1 = new TestListener();
        var listener2 = new TestListener();
        var listener3 = new TestListener();
        var event = new Event("Hello");
        eventBus.post(event);
        Assert.assertTrue(
                out.toString().contains("event fired for 3 recipients."));
        Assert.assertEquals(1, listener1.getEventCount());
        Assert.assertEquals("Hello", listener1.getLastEvent().toString());
        Assert.assertEquals(1, listener2.getEventCount());
        Assert.assertEquals("Hello", listener2.getLastEvent().toString());

        listener1.remove();
        listener3 = null;

        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }

        event = new Event("World");
        eventBus.post(event);
        Assert.assertEquals(1, listener1.getEventCount());
        Assert.assertEquals("Hello", listener1.getLastEvent().toString());
        Assert.assertEquals(2, listener2.getEventCount());
        Assert.assertEquals("World", listener2.getLastEvent().toString());
        Assert.assertTrue(
                out.toString().contains("event fired for 1 recipients."));

        listener2.remove();
    }

    record Event(String message) {
        @Override
        public final String toString() {
            return message;
        }
    }

    public static class TestListener implements EventBusListener {

        private AtomicInteger count = new AtomicInteger(0);
        private Object event;
        private EventBus eventBus = EventBus.get();

        public TestListener() {
            eventBus.registerEventBusListener(this);
        }

        @Override
        public void eventFired(Object event) {
            count.incrementAndGet();
            this.event = event;
        }

        public int getEventCount() {
            return count.get();
        }

        public Object getLastEvent() {
            return event;
        }

        public void remove() {
            eventBus.unregisterEventBusListener(this);
        }
    }
}
