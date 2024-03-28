package org.vaadin.tatu.vaadincreate.uiunittest.mocks;

import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

@SuppressWarnings("serial")
public class MockUI extends UI {

    public MockUI() throws ServiceException {
        this(findOrcreateSession());
    }

    public MockUI(VaadinSession session) {
        setSession(session);
        setCurrent(this);
    }

    @Override
    protected void init(VaadinRequest request) {
        // Do nothing
    }

    private static VaadinSession findOrcreateSession() throws ServiceException {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            session = new AlwaysLockedVaadinSession(new MockVaadinService());
            VaadinSession.setCurrent(session);
        }
        return session;
    }

    public static class AlwaysLockedVaadinSession extends MockVaadinSession {

        public AlwaysLockedVaadinSession(VaadinService service) {
            super(service);
            lock();
        }
    }
}
