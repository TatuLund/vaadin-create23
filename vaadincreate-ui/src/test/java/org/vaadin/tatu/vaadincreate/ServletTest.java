package org.vaadin.tatu.vaadincreate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.jsoup.Jsoup;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.i18n.DefaultI18NProvider;

import com.vaadin.server.BootstrapHandler;
import com.vaadin.server.BootstrapPageResponse;
import com.vaadin.server.ErrorEvent;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.testbench.uiunittest.UIUnitTest;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

public class ServletTest extends UIUnitTest {

    @Test
    public void deployment_configuration_returns_config_with_production_mode_when_system_property_is_set() {
        System.setProperty("vaadin.productionMode", "true");
        var initParameters = new Properties();
        var servlet = new Servlet();
        assertTrue(servlet.createDeploymentConfiguration(initParameters)
                .isProductionMode());
    }

    @Test
    public void deployment_configuration_returns_config_with_production_mode_false_when_system_property_is_not_set() {
        System.clearProperty("vaadin.productionMode");
        var initParameters = new Properties();
        var servlet = new Servlet();
        assertTrue(!servlet.createDeploymentConfiguration(initParameters)
                .isProductionMode());
    }

    @Test
    public void handleRequest_saves_selected_language_response_locale()
            throws ServiceException {
        // Arrange
        var servlet = new Servlet();
        mockVaadin();
        var session = VaadinSession.getCurrent();
        session.setAttribute("locale",
                DefaultI18NProvider.LOCALE_FI.getLanguage());

        // Act
        servlet.handleRequest(VaadinSession.getCurrent(),
                VaadinRequest.getCurrent(), VaadinResponse.getCurrent());
        var locale = ((VaadinServletResponse) VaadinResponse.getCurrent())
                .getLocale();

        // Assert
        assertNotNull(locale);
        assertEquals(DefaultI18NProvider.LOCALE_FI.getLanguage(),
                locale.getLanguage());
    }

    @Test
    public void handleError_displays_error_notification()
            throws ServiceException {
        // Arrange
        var ui = new VaadinCreateUI();
        mockVaadin(ui);
        var servlet = new Servlet();
        var exception = new Exception("Test exception");
        var session = VaadinSession.getCurrent();
        Locale.setDefault(DefaultI18NProvider.LOCALE_EN);

        // Act
        servlet.handleError(new ErrorEvent(exception), session);
        waitWhile(Notification.class,
                not -> $(Notification.class).first() != null, 1);

        var notification = $(Notification.class).first();
        assertEquals("Exception happened: \"#0000000001\"",
                notification.getCaption());
        assertEquals("Test exception", notification.getDescription());

        // Assert
        assertTrue(servlet.exceptionCount.get() > 0);
    }

    @Test
    public void bootstrapPageResponse_sets_html_lang_attribute()
            throws ServiceException {
        // Arrange
        mockVaadin();
        var bootstrapListener = new Servlet.LocaleBootstrapListener();
        var request = VaadinRequest.getCurrent();
        var mockRequest = new MockRequest((HttpServletRequest) request,
                VaadinService.getCurrent());
        var response = new MockBootstrapResponse(mockRequest,
                VaadinSession.getCurrent(), UI.getCurrent());

        // Act
        bootstrapListener.modifyBootstrapPage(response);

        // Assert
        assertEquals(DefaultI18NProvider.LOCALE_FI.getLanguage(),
                response.getDocument().getElementsByTag("html").attr("lang"));
    }

    public static class MockRequest extends VaadinServletRequest {

        public MockRequest(HttpServletRequest request,
                VaadinService vaadinService) {
            super(request, (VaadinServletService) vaadinService);
        }

        @Override
        public Cookie[] getCookies() {
            return new Cookie[] {
                    Servlet.createNewCookie(this, new StringBuilder("fi")) };
        }
    }

    public static class MockBootstrapResponse extends BootstrapPageResponse {

        private static final String HTML = "<html><head></head><body></body></html>";

        public MockBootstrapResponse(VaadinRequest request,
                VaadinSession session, UI ui) {
            super(new MockBootstrapHandler(), request, session, ui.getClass(),
                    Jsoup.parse(HTML), new HashMap<>(), null);
        }

    }

    @SuppressWarnings("deprecation")
    public static class MockBootstrapHandler extends BootstrapHandler {

        @Override
        protected String getContextRootPath(BootstrapContext context) {
            throw new UnsupportedOperationException(
                    "Unimplemented method 'getContextRootPath'");
        }

        @Override
        protected String getServiceUrl(BootstrapContext context) {
            throw new UnsupportedOperationException(
                    "Unimplemented method 'getServiceUrl'");
        }
    }
}
