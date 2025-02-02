package org.vaadin.tatu.vaadincreate.locking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.backend.UserService;
import org.vaadin.tatu.vaadincreate.backend.data.AbstractEntity;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;
import org.vaadin.tatu.vaadincreate.locking.LockedObjects.LockingEvent;

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
        var listener = new TestListener();
        var user = userService.getAllUsers().get(0);

        lockedObjects.lock(objects.get(0), user);
        try {
            latch.await();
        } catch (InterruptedException e) {
            // Ignore
        }

        assertEquals(user, lockedObjects.isLocked(objects.get(0)));
        var event = listener.getLastEvent();
        assertEquals(1, listener.getEventCount());
        assertEquals(user, event.user());
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

        assertEquals(null, lockedObjects.isLocked(objects.get(0)));
        event = listener.getLastEvent();
        assertEquals(2, listener.getEventCount());
        assertEquals(user, event.user());
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
        assertEquals(user, lockedObjects.isLocked(objects.get(0)));
        try {
            lockedObjects.lock(objects.get(0),
                    userService.getAllUsers().get(1));
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);
        assertEquals(user, lockedObjects.isLocked(objects.get(0)));
        lockedObjects.unlock(objects.get(0));
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
            latch.countDown();
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

    public static class MockObject extends AbstractEntity {

        public MockObject(int id) {
            setId(id);
        }

    }
}
