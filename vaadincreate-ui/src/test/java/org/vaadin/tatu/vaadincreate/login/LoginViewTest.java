package org.vaadin.tatu.vaadincreate.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;

import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.auth.MockAccessControl;
import org.vaadin.tatu.vaadincreate.i18n.DefaultI18NProvider;

import com.vaadin.testbench.uiunittest.UIUnitTest;

import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

public class LoginViewTest extends UIUnitTest {

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
    public void loginEventFired() {
        var count = new AtomicInteger(0);
        var accessControl = new MockAccessControl("Admin");
        assertFalse(accessControl.isUserSignedIn());
        var login = new LoginView(accessControl, e -> count.addAndGet(1));
        ui.setContent(login);

        test(login.usernameField).setValue("Admin");
        test(login.passwordField).setValue("Admin");
        test($(LanguageSelect.class).first())
                .clickItem(DefaultI18NProvider.LOCALE_EN);
        test(login.login).click();
        assertEquals(1, count.get());
        assertTrue(accessControl.isUserSignedIn());
        assertEquals(DefaultI18NProvider.LOCALE_EN.getLanguage(),
                VaadinSession.getCurrent().getAttribute("locale"));
    }

    @Test
    public void loginEventNotFired() {
        var count = new AtomicInteger(0);
        var accessControl = new MockAccessControl("Admin");
        assertFalse(accessControl.isUserSignedIn());
        var login = new LoginView(accessControl, e -> count.addAndGet(1));
        ui.setContent(login);
        test($(login, LanguageSelect.class).single())
                .clickItem(DefaultI18NProvider.LOCALE_EN);

        test(login.usernameField).setValue("Admin");
        test(login.passwordField).setValue("Wrong");
        test(login.login).click();
        assertEquals(0, count.get());
        assertEquals("Login failed", $(Notification.class).last().getCaption());
        assertFalse(accessControl.isUserSignedIn());
    }

    @Test
    public void resizeTest() {
        var login = createLogin();
        assertDefaults(login);

        ui.getPage().updateBrowserWindowSize(600, 800, true);
        assertEquals("", login.usernameField.getCaption());
        assertEquals("", login.passwordField.getCaption());
        assertEquals("", $(login, LanguageSelect.class).single().getCaption());
        assertEquals("Username", login.usernameField.getPlaceholder());
        assertEquals("Password", login.passwordField.getPlaceholder());
        assertNull($(CssLayout.class)
                .styleName(VaadinCreateTheme.LOGINVIEW_INFORMATION).first());

        ui.getPage().updateBrowserWindowSize(1024, 800, true);
        assertDefaults(login);
    }

    private void assertDefaults(LoginView login) {
        assertEquals("Username", login.usernameField.getCaption());
        assertEquals("Password", login.passwordField.getCaption());
        assertEquals("Language",
                $(login, LanguageSelect.class).single().getCaption());
        assertEquals("", login.usernameField.getPlaceholder());
        assertEquals("", login.passwordField.getPlaceholder());
        assertNotNull($(CssLayout.class)
                .styleName(VaadinCreateTheme.LOGINVIEW_INFORMATION).first());
    }

    @Test
    public void changeLanguage() {
        var login = createLogin();

        assertEquals("Username", login.usernameField.getCaption());
        assertEquals("Password", login.passwordField.getCaption());
        assertEquals("Log in", login.login.getCaption());
        assertEquals("Language",
                $(login, LanguageSelect.class).single().getCaption());

        test($(login, LanguageSelect.class).single())
                .clickItem(DefaultI18NProvider.LOCALE_DE);
        assertEquals("Benutzername", login.usernameField.getCaption());
        assertEquals("Passwort", login.passwordField.getCaption());
        assertEquals("Anmelden", login.login.getCaption());
        assertEquals("Sprache",
                $(login, LanguageSelect.class).single().getCaption());

        ui.getPage().updateBrowserWindowSize(600, 800, true);

        test($(login, LanguageSelect.class).single())
                .clickItem(DefaultI18NProvider.LOCALE_EN);
        assertEquals("Username", login.usernameField.getPlaceholder());
        assertEquals("Password", login.passwordField.getPlaceholder());
        assertEquals("Log in", login.login.getCaption());
        assertEquals("", $(login, LanguageSelect.class).single().getCaption());
    }

    private LoginView createLogin() {
        var accessControl = new MockAccessControl("Admin");
        var login = new LoginView(accessControl, e -> {
        });
        ui.setContent(login);
        test($(login, LanguageSelect.class).single())
                .clickItem(DefaultI18NProvider.LOCALE_EN);
        return login;
    }

    @Test
    public void loginHint() {
        var login = createLogin();

        $(login, Button.class)
                .styleName(VaadinCreateTheme.LOGINVIEW_FORGOTBUTTON).single()
                .click();
        assertEquals("Hint: Try User0 / user0 or Admin / admin",
                $(Notification.class).last().getCaption());
    }
}
