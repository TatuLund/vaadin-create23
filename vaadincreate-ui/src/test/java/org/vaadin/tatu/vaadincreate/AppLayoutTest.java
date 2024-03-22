package org.vaadin.tatu.vaadincreate;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class AppLayoutTest {

    @Test
    public void testAppLayoutAccessControlAdminPass() throws ServiceException {
        // Vaadin mocks
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

        // App mocks
        var accessControl = new MockAccessControl("Admin");
        accessControl.signIn("Admin", "Admin");
        var appLayout = new AppLayout(ui, accessControl);
        appLayout.addView(MockView.class, "Test", VaadinIcons.INFO, "test");
        ui.setContent(appLayout);

        // Test
        ui.getNavigator().navigateTo("test");
        var view = (MockView) ui.getNavigator().getCurrentView();
        Assert.assertTrue(view.label != null);
        Assert.assertEquals("View", view.label.getValue());
    }

    @Test
    public void testAppLayoutAccessControlUserFail() throws ServiceException {
        // Vaadin Mocks
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

        // App mocks
        var accessControl = new MockAccessControl("User");
        accessControl.signIn("User", "User");
        var appLayout = new AppLayout(ui, accessControl);
        appLayout.addView(MockView.class, "Test", VaadinIcons.INFO, "test");

        // Test
        ui.setContent(appLayout);
        ui.getNavigator().navigateTo("test");
        var view = (ErrorView) ui.getNavigator().getCurrentView();
    }

    @RolesPermitted({ Role.ADMIN })
    public static class MockView extends VerticalLayout implements View {

        Label label = null;

        public MockView() {
        }

        @Override
        public void enter(ViewChangeEvent event) {
            label = new Label("View");
            addComponent(label);
        }
    }

    @SuppressWarnings("serial")
    public static class MockAccessControl implements AccessControl {

        private boolean signedIn = false;
        private String principal;

        public MockAccessControl(String principal) {
            this.principal = principal;
        }

        @Override
        public boolean signIn(String username, String password) {
            signedIn = username.equals(principal) && username.equals(password);
            CurrentUser.set(new User(1, principal, principal,
                    principal.equals("Admin") ? Role.ADMIN : Role.USER));
            return signedIn;
        }

        @Override
        public boolean isUserSignedIn() {
            return signedIn;
        }

        @Override
        public boolean isUserInRole(Role role) {
            return (role == Role.ADMIN && principal.equals("Admin"))
                    || role == Role.USER;
        }

        @Override
        public String getPrincipalName() {
            return principal;
        }

    }

}