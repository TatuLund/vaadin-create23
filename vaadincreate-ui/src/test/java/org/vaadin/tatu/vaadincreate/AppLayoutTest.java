package org.vaadin.tatu.vaadincreate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AppLayout.MenuButton;
import org.vaadin.tatu.vaadincreate.auth.MockAccessControl;
import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.i18n.DefaultI18NProvider;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;

import com.vaadin.testbench.uiunittest.UIUnitTest;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ServiceException;
import com.vaadin.shared.Position;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class AppLayoutTest extends UIUnitTest {

    UI ui;

    @Before
    public void setup() throws ServiceException {
        // Vaadin mocks
        ui = mockVaadin();
        ui.getSession().setLocale(DefaultI18NProvider.LOCALE_EN);
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
        var menuItem = $(MenuButton.class).single();
        assertFalse(menuItem.getStyleName().contains(ValoTheme.MENU_SELECTED));

        // Test
        accessControl.signIn("Admin", "Admin");
        var view = navigate("test", MockView.class);

        assertNotNull(view.label);
        assertEquals("View", view.label.getValue());
        assertTrue(menuItem.getStyleName().contains(ValoTheme.MENU_SELECTED));
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

    @Test
    public void menuToggleButton() {
        // App mocks
        var accessControl = new MockAccessControl("Admin");
        mockApp(ui, accessControl);

        var button = $(Button.class).styleName(ValoTheme.MENU_TOGGLE).single();
        test(button).click();
        var menu = $(CssLayout.class).styleName(ValoTheme.MENU_PART).single();
        assertTrue(menu.getStyleName().contains(ValoTheme.MENU_VISIBLE));
        var notification = $(Notification.class).last();
        assertEquals("navigation menu opened", notification.getCaption());
        assertEquals(Position.ASSISTIVE, notification.getPosition());
        test(button).click();
        assertFalse(menu.getStyleName().contains(ValoTheme.MENU_VISIBLE));
        $(Notification.class).last();
        assertEquals("navigation menu closed",
                $(Notification.class).last().getCaption());
        assertEquals(Position.ASSISTIVE, notification.getPosition());
    }

    @Test(expected = IllegalStateException.class)
    public void testAppLayoutAccessControlNoAnnotation() {
        // App mocks
        var accessControl = new MockAccessControl("Admin");
        badMockApp(ui, accessControl);

        // Test
        accessControl.signIn("Admin", "Admin");
        ui.getNavigator().navigateTo("test");
    }

    private void mockApp(UI ui, MockAccessControl accessControl) {
        var appLayout = new AppLayout(ui, accessControl);
        appLayout.addView(MockView.class, "Test", VaadinIcons.INFO, "test");
        ui.setContent(appLayout);
    }

    private void badMockApp(UI ui, MockAccessControl accessControl) {
        var appLayout = new AppLayout(ui, accessControl);
        appLayout.addView(UnAnnotated.class, "Test", VaadinIcons.INFO, "test");
        ui.setContent(appLayout);
    }

    @SuppressWarnings("serial")
    @RolesPermitted({ Role.ADMIN })
    public static class MockView extends VerticalLayout
            implements View, HasI18N {

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

    @SuppressWarnings("serial")
    public static class UnAnnotated extends VerticalLayout
            implements View, HasI18N {

        Label label = null;

        public UnAnnotated() {
            // public no arg constructor
        }

        @Override
        public void enter(ViewChangeEvent event) {
            label = new Label("View");
            addComponent(label);
        }
    }

}