package org.vaadin.tatu.vaadincreate;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.auth.MockAccessControl;
import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import com.vaadin.testbench.uiunittest.UIUnitTest;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ServiceException;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class AppLayoutTest extends UIUnitTest {

    UI ui;

    @Before
    public void setup() throws ServiceException {
        // Vaadin mocks
        ui = mockVaadin();
    }

    @After
    public void cleanup() {
        tearDown();
    }

    @Test
    public void testAppLayoutAccessControlAdminPass() {
        // App mocks
        var accessControl = new MockAccessControl("Admin");
        mockApp(ui, accessControl);

        // Test
        accessControl.signIn("Admin", "Admin");
        var view = navigate("test", MockView.class);

        Assert.assertNotNull(view.label);
        Assert.assertEquals("View", view.label.getValue());
    }

    @Test
    public void testAppLayoutAccessControlUserFail() {
        // App mocks
        var accessControl = new MockAccessControl("User");
        mockApp(ui, accessControl);

        // Test
        accessControl.signIn("User", "User");
        ui.getNavigator().navigateTo("test");
        var view = (ErrorView) ui.getNavigator().getCurrentView();
        assertNotNull(view);
    }

    @Test
    public void testAppLayoutTestUnknownRoute() {
        // App mocks
        var accessControl = new MockAccessControl("Admin");
        mockApp(ui, accessControl);

        // Test
        accessControl.signIn("Admin", "Admin");
        ui.getNavigator().navigateTo("nonview");

        var view = (ErrorView) ui.getNavigator().getCurrentView();
        assertNotNull(view);
    }

    private void mockApp(UI ui, MockAccessControl accessControl) {
        var appLayout = new AppLayout(ui, accessControl);
        appLayout.addView(MockView.class, "Test", VaadinIcons.INFO, "test");
        ui.setContent(appLayout);
    }

    @SuppressWarnings("serial")
    @RolesPermitted({ Role.ADMIN })
    public static class MockView extends VerticalLayout implements View {

        Label label = null;

        public MockView() {
            // public no arg constructor
        }

        @Override
        public void enter(ViewChangeEvent event) {
            label = new Label("View");
            addComponent(label);
        }
    }
}