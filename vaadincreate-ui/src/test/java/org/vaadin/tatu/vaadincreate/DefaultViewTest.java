package org.vaadin.tatu.vaadincreate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.i18n.DefaultI18NProvider;
import org.vaadin.tatu.vaadincreate.i18n.I18NProvider;
import org.vaadin.tatu.vaadincreate.util.CookieUtils;

import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;

public class DefaultViewTest extends AbstractUITest {

    private VaadinCreateUI ui;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        var user = ui.getUserService().findByName("Admin").get();
        getSession().setAttribute(
                CurrentUser.CURRENT_USER_SESSION_ATTRIBUTE_KEY, user);
        mockVaadin(ui);
        var locale = I18NProvider.fetchLocaleFromCookie();
        assertEquals("fi", locale.getLanguage());
        ui.getSession().setLocale(
                locale != null ? locale : Locale.ENGLISH);
        navigate("", AboutView.class);
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
    }

    @Test
    public void if_session_has_user_default_route_is_opened() {
        var aboutView = $(AboutView.class).single();
        assertNotNull(aboutView);
        assertEquals(1, VaadinRequest.getCurrent().getCookies().length);
        assertAssistiveNotification("Tietoja avattu");
    }

    @Override
    protected VaadinServletRequest getVaadinRequest() throws ServiceException {
        var request = super.getVaadinRequest();
        return new MockRequest(request,
                (VaadinServletService) VaadinServletService.getCurrent());
    }

    public static class MockRequest extends VaadinServletRequest {

        public MockRequest(HttpServletRequest request,
                VaadinServletService vaadinService) {
            super(request, vaadinService);
        }

        @Override
        public Cookie[] getCookies() {
            Cookie cookie = CookieUtils.createNewCookie(this,
                    DefaultI18NProvider.LOCALE_FI.getLanguage());
            return new Cookie[] { cookie };
        }
    }
}
