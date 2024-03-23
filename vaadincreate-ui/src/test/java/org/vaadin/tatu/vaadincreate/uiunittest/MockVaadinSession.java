package org.vaadin.tatu.vaadincreate.uiunittest;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;

@SuppressWarnings("serial")
public class MockVaadinSession extends VaadinSession {
    /*
     * Used to make sure there's at least one reference to the mock session
     * while it's locked. This is used to prevent the session from being eaten
     * by GC in tests where @Before creates a session and sets it as the current
     * instance without keeping any direct reference to it. This pattern has a
     * chance of leaking memory if the session is not unlocked in the right way,
     * but it should be acceptable for testing use.
     */
    private static final ThreadLocal<MockVaadinSession> referenceKeeper = new ThreadLocal<>();

    public MockVaadinSession(VaadinService service) {
        super(service);
    }

    @Override
    public void close() {
        super.close();
        closeCount++;
    }

    public int getCloseCount() {
        return closeCount;
    }

    @Override
    public Lock getLockInstance() {
        return lock;
    }

    @Override
    public void lock() {
        super.lock();
        referenceKeeper.set(this);
    }

    @Override
    public void unlock() {
        super.unlock();
        referenceKeeper.remove();
    }

    private int closeCount;

    private ReentrantLock lock = new ReentrantLock();
}
