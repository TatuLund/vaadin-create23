package org.vaadin.tatu.vaadincreate.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;

import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.auth.MockAccessControl;
import org.vaadin.tatu.vaadincreate.i18n.DefaultI18NProvider;

import com.vaadin.testbench.uiunittest.UIUnitTest;

import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinSession;
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

        test(login.username).setValue("Admin");
        test(login.password).setValue("Admin");
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

        test(login.username).setValue("Admin");
        test(login.password).setValue("Wrong");
        test(login.login).click();
        assertEquals(0, count.get());
        assertEquals("Login failed", $(Notification.class).last().getCaption());
        assertFalse(accessControl.isUserSignedIn());
    }
}
