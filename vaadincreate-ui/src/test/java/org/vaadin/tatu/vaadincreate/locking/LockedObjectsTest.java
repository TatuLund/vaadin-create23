package org.vaadin.tatu.vaadincreate.locking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.backend.UserService;
import org.vaadin.tatu.vaadincreate.backend.data.AbstractEntity;
import org.vaadin.tatu.vaadincreate.backend.events.AbstractEvent;
import org.vaadin.tatu.vaadincreate.backend.events.LockingEvent;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;

public class LockedObjectsTest {

    LockedObjects lockedObjects = LockedObjects.get();
    UserService userService = UserService.get();

    List<MockObject> objects;
    static CountDownLatch latch = new CountDownLatch(1);

    @Before
    public void init() {
        objects = IntStream.range(0, 10).mapToObj(MockObject::new).toList();
    }

    @Test
    public void lockAndUnlockObject() {
        latch = new CountDownLatch(1);
        var listener = new TestListener();
        var user = userService.getAllUsers().get(0);

        lockedObjects.lock(objects.get(0), user);
        try {
            latch.await();
        } catch (InterruptedException e) {
            // Ignore
        }

        assertEquals(user.getName(), lockedObjects.isLocked(objects.get(0)));
        var event = listener.getLastEvent();
        assertEquals(1, listener.getEventCount());
        assertEquals(user.getName(), event.userName());
        assertEquals(objects.get(0).getId(), event.id());
        assertEquals(MockObject.class, event.type());
        assertTrue(event.locked());

        latch = new CountDownLatch(1);
        lockedObjects.unlock(objects.get(0));
        try {
            latch.await();
        } catch (InterruptedException e) {
            // Ignore
        }

        assertNull(lockedObjects.isLocked(objects.get(0)));
        event = listener.getLastEvent();
        assertEquals(2, listener.getEventCount());
        assertEquals(user.getId(), event.userId());
        assertEquals(user.getName(), event.userName());
        assertEquals(objects.get(0).getId(), event.id());
        assertEquals(MockObject.class, event.type());
        assertFalse(event.locked());
        listener.remove();
    }

    @Test
    public void lockingTwiceThrows() {
        boolean thrown = false;
        var user = userService.getAllUsers().get(0);
        lockedObjects.lock(objects.get(0), user);
        assertEquals(user.getName(), lockedObjects.isLocked(objects.get(0)));
        try {
            lockedObjects.lock(objects.get(0),
                    userService.getAllUsers().get(1));
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);
        assertEquals(user.getName(), lockedObjects.isLocked(objects.get(0)));
        lockedObjects.unlock(objects.get(0));
    }

    @Test
    public void remoteLockingAndUnlocking() {
        latch = new CountDownLatch(1);
        var listener = new TestListener();
        var user = userService.getAllUsers().get(0);
        var eventBus = EventBus.get();
        var lockingEvent = new LockingEvent(MockObject.class,
                objects.get(0).getId(), user.getId(), user.getName(), true);
        eventBus.post(lockingEvent);
        try {
            latch.await();
        } catch (InterruptedException e) {
            // Ignore
        }
        assertEquals(1, listener.getEventCount());

        assertEquals(user.getName(), lockedObjects.isLocked(objects.get(0)));

        latch = new CountDownLatch(1);
        lockingEvent = new LockingEvent(MockObject.class,
                objects.get(0).getId(), user.getId(), user.getName(), false);
        eventBus.post(lockingEvent);
        try {
            latch.await();
        } catch (InterruptedException e) {
            // Ignore
        }
        assertNull(lockedObjects.isLocked(objects.get(0)));
    }

    public static class TestListener implements EventBusListener {

        private AtomicInteger count = new AtomicInteger(0);
        private LockingEvent event;
        private EventBus eventBus = EventBus.get();

        public TestListener() {
            eventBus.registerEventBusListener(this);
        }

        @Override
        public void eventFired(AbstractEvent event) {
            count.incrementAndGet();
            if (event instanceof LockingEvent) {
                this.event = (LockingEvent) event;
            }
            latch.countDown();
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

    public static class MockObject extends AbstractEntity {

        public MockObject(int id) {
            setId(id);
        }

    }
}
