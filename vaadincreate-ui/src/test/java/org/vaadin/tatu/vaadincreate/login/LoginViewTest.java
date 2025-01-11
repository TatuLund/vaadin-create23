package org.vaadin.tatu.vaadincreate.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
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
    public void login_event_is_fired_when_login_passes_using_correct_credentials() {
        var count = new AtomicInteger(0);
        var accessControl = new MockAccessControl("Admin");
        assertFalse(accessControl.isUserSignedIn());
        var login = new LoginView(accessControl,
                loginEvent -> count.addAndGet(1));
        ui.setContent(login);

        // WHEN: user logs in with correct credentials and language is set to
        // English
        test(login.usernameField).setValue("Admin");
        test(login.passwordField).setValue("Admin");
        test($(LanguageSelect.class).first())
                .clickItem(DefaultI18NProvider.LOCALE_EN);
        test(login.login).click();

        // THEN: login event should be fired and user should be signed in and
        // locale should be set to English
        assertEquals(1, count.get());
        assertTrue(accessControl.isUserSignedIn());
        assertEquals(DefaultI18NProvider.LOCALE_EN.getLanguage(),
                VaadinSession.getCurrent().getAttribute("locale"));
        assertEquals(DefaultI18NProvider.LOCALE_EN, Locale.getDefault());
        assertEquals(DefaultI18NProvider.LOCALE_EN,
                VaadinSession.getCurrent().getLocale());
    }

    @Test
    public void login_event_is_not_fired_when_wrong_credentials_are_used() {
        var count = new AtomicInteger(0);
        var accessControl = new MockAccessControl("Admin");
        assertFalse(accessControl.isUserSignedIn());
        var login = new LoginView(accessControl, e -> count.addAndGet(1));
        ui.setContent(login);
        test($(login, LanguageSelect.class).single())
                .clickItem(DefaultI18NProvider.LOCALE_EN);

        // WHEN: user logs in with wrong credentials
        test(login.usernameField).setValue("Admin");
        test(login.passwordField).setValue("Wrong");
        test(login.login).click();

        // THEN: login event should not be fired and user should not be signed
        assertEquals(0, count.get());
        assertEquals("Login failed", $(Notification.class).last().getCaption());
        assertFalse(accessControl.isUserSignedIn());
    }

    @Test
    public void when_browser_is_small_placeholders_are_used_and_when_browser_is_large_captions_are_used() {
        var login = createLogin();
        then_caption_are_set_and_placeholders_are_not_and_information_text_is_shown(
                login);

        // WHEN: resizing the browser window to be smaller
        ui.getPage().updateBrowserWindowSize(600, 800, true);

        // THEN: placeholders should be shown and captions should be
        // empty
        assertEquals("", login.usernameField.getCaption());
        assertEquals("", login.passwordField.getCaption());
        assertEquals("", $(login, LanguageSelect.class).single().getCaption());
        assertEquals("Username", login.usernameField.getPlaceholder());
        assertEquals("Password", login.passwordField.getPlaceholder());
        // THEN: information should not be shown
        assertNull($(CssLayout.class)
                .styleName(VaadinCreateTheme.LOGINVIEW_INFORMATION).first());

        // WHEN: resizing the browser window to be larger
        ui.getPage().updateBrowserWindowSize(1024, 800, true);

        then_caption_are_set_and_placeholders_are_not_and_information_text_is_shown(
                login);
    }

    private void then_caption_are_set_and_placeholders_are_not_and_information_text_is_shown(
            LoginView login) {
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
    public void changing_language_will_update_localized_texts_used_in_login_view() {
        var login = createLogin();

        assertEquals("Username", login.usernameField.getCaption());
        assertEquals("Password", login.passwordField.getCaption());
        assertEquals("Log in", login.login.getCaption());
        assertEquals("Language",
                $(login, LanguageSelect.class).single().getCaption());

        // WHEN: changing language to German
        test($(login, LanguageSelect.class).single())
                .clickItem(DefaultI18NProvider.LOCALE_DE);

        // THEN: localized texts should be updated to German
        assertEquals("Benutzername", login.usernameField.getCaption());
        assertEquals("Passwort", login.passwordField.getCaption());
        assertEquals("Anmelden", login.login.getCaption());
        assertEquals("Sprache",
                $(login, LanguageSelect.class).single().getCaption());

        // THEN: locale should be set to German
        assertEquals(DefaultI18NProvider.LOCALE_DE, Locale.getDefault());
        assertEquals(DefaultI18NProvider.LOCALE_DE,
                VaadinSession.getCurrent().getLocale());

        // WHEN: resizing the browser window to be smaller and changing
        // language to English
        ui.getPage().updateBrowserWindowSize(600, 800, true);
        test($(login, LanguageSelect.class).single())
                .clickItem(DefaultI18NProvider.LOCALE_EN);

        // THEN: localized texts should be updated to English and
        // placeholders should be shown
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
    public void login_hint_notification_is_displayed_when_hint_button_is_clicked() {
        var login = createLogin();

        // WHEN: clicking the hint button
        $(login, Button.class)
                .styleName(VaadinCreateTheme.LOGINVIEW_FORGOTBUTTON).single()
                .click();

        // THEN: hint notification should be shown
        assertEquals("Hint: Try User0 / user0 or Admin / admin",
                $(Notification.class).last().getCaption());
    }
}
