package org.vaadin.tatu.vaadincreate.locking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.backend.UserService;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;
import org.vaadin.tatu.vaadincreate.locking.LockedObjects.LockingEvent;

public class LockedObjectsTest {

    LockedObjects lockedObjects = LockedObjects.get();
    UserService userService = UserService.get();

    List<MockObject> objects;

    @Before
    public void init() {
        objects = IntStream.range(0, 10).mapToObj(i -> new MockObject(i))
                .collect(Collectors.toList());
    }

    @Test
    public void lockAndUnlockObject() {
        var listener = new TestListener();
        var user = userService.getAllUsers().get(0);

        lockedObjects.lock(MockObject.class, objects.get(0).getId(), user);

        assertEquals(user, lockedObjects.isLocked(MockObject.class,
                objects.get(0).getId()));
        var event = listener.getLastEvent();
        assertEquals(1, listener.getEventCount());
        assertEquals(user, event.getUser());
        assertEquals(objects.get(0).getId(), (int) event.getId());
        assertEquals(MockObject.class, event.getType());
        assertTrue(event.isLocked());

        lockedObjects.unlock(MockObject.class, objects.get(0).getId());

        assertEquals(null, lockedObjects.isLocked(MockObject.class,
                objects.get(0).getId()));
        event = listener.getLastEvent();
        assertEquals(2, listener.getEventCount());
        assertEquals(user, event.getUser());
        assertEquals(objects.get(0).getId(), (int) event.getId());
        assertEquals(MockObject.class, event.getType());
        assertFalse(event.isLocked());

        listener.remove();
    }

    @Test
    public void lockLedgerIsWeak() {
        var listener = new TestListener();
        var user = userService.getAllUsers().get(0);
        var id = objects.get(0).getId();
        lockedObjects.lock(MockObject.class, id, user);

        assertEquals(user, lockedObjects.isLocked(MockObject.class, id));
        var event = listener.getLastEvent();
        assertEquals(1, listener.getEventCount());
        assertEquals(user, event.getUser());
        assertEquals(id, (int) event.getId());
        assertEquals(MockObject.class, event.getType());
        assertTrue(event.isLocked());

        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }

        assertEquals(null, lockedObjects.isLocked(MockObject.class, id));

        listener.remove();
    }

    public void lockingTwiceThrows() {
        boolean thrown = false;
        var user = userService.getAllUsers().get(0);
        lockedObjects.lock(MockObject.class, objects.get(0).getId(), user);
        assertEquals(user, lockedObjects.isLocked(MockObject.class,
                objects.get(0).getId()));
        try {
            lockedObjects.lock(MockObject.class, objects.get(0).getId(),
                    userService.getAllUsers().get(1));
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);
        assertEquals(user, lockedObjects.isLocked(MockObject.class,
                objects.get(0).getId()));
        lockedObjects.unlock(MockObject.class, objects.get(0).getId());
    }

    public static class TestListener implements EventBusListener {

        private AtomicInteger count = new AtomicInteger(0);
        private LockingEvent event;
        private EventBus eventBus = EventBus.get();

        public TestListener() {
            eventBus.registerEventBusListener(this);
        }

        @Override
        public void eventFired(Object event) {
            count.incrementAndGet();
            this.event = (LockingEvent) event;
        }

        public int getEventCount() {
            return count.get();
        }

        public LockingEvent getLastEvent() {
            return event;
        }

        public void remove() {
            eventBus.unregisterEventBusListener(this);
        }
    }

    public static class MockObject {
        private int id;

        public MockObject(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }
}
