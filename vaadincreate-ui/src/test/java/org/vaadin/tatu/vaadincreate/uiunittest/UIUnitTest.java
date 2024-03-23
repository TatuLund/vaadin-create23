package org.vaadin.tatu.vaadincreate.uiunittest;

import javax.servlet.http.HttpServletRequest;

import org.mockito.Mockito;

import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

/**
 * Base class for unit testing complex Vaadin components.
 */
public abstract class UIUnitTest {

    /**
     * Create mocked Vaadin environment without Atmosphere support. This is
     * enough for common use cases. In Vaadin 23/24 version of the TestBench
     * there is more complete Vaadin environment mock together with component
     * helpers. Will set UI and Session thread locals.
     *
     * @throws ServiceException
     */
    public void mockVaadin() throws ServiceException {
        var session = new MockVaadinSession(new MockVaadinService());
        session.lock();
        VaadinSession.setCurrent(session);
        var ui = new MockUI();
        UI.setCurrent(ui);
        var request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getParameter("v-loc")).thenReturn("");
        Mockito.when(request.getParameter("v-cw")).thenReturn("1280");
        Mockito.when(request.getParameter("v-ch")).thenReturn("800");
        Mockito.when(request.getParameter("v-wn")).thenReturn("window");
        ui.getPage().init(new VaadinServletRequest(request,
                (VaadinServletService) session.getService()));
    }
}
