package org.vaadin.tatu.vaadincreate;

import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.DatabaseConnectionException;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.observability.Telemetry;
import org.vaadin.tatu.vaadincreate.util.CookieUtils;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.BootstrapFragmentResponse;
import com.vaadin.server.BootstrapListener;
import com.vaadin.server.BootstrapPageResponse;
import com.vaadin.server.CustomizedSystemMessages;
import com.vaadin.server.DefaultDeploymentConfiguration;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ErrorEvent;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinSession;

// Set maxIdleTime because of Jetty 10, see:
// https://github.com/vaadin/flow/issues/17215
@WebServlet(value = "/*", asyncSupported = true, initParams = {
        @WebInitParam(name = "org.atmosphere.cpr.AtmosphereConfig.getInitParameter", value = "true"),
        @WebInitParam(name = "org.atmosphere.websocket.maxIdleTime", value = "300000") })
@VaadinServletConfiguration(productionMode = false, ui = VaadinCreateUI.class, heartbeatInterval = 60, closeIdleSessions = true, widgetset = "org.vaadin.tatu.vaadincreate.VaadinCreateWidgetSet")
public class Servlet extends VaadinServlet {

    static final class LocaleBootstrapListener implements BootstrapListener {
        @Override
        public void modifyBootstrapPage(BootstrapPageResponse response) {
            Cookie localeCookie = CookieUtils.getCookieByName(
                    CookieUtils.COOKIE_LANGUAGE, response.getRequest());
            response.getDocument().getElementsByTag("html").get(0).attributes()
                    .add("lang", localeCookie != null ? localeCookie.getValue()
                            : "en");
            var contextPath = response.getRequest().getContextPath();
            var manifestPath = contextPath + "/manifest.webmanifest";
            var swPath = contextPath + "/sw.js";
            var icon192 = contextPath + "/icons/icon-192.png";
            var icon512 = contextPath + "/icons/icon-512.png";

            var head = response.getDocument().head();
            head.appendElement("link").attr("rel", "manifest").attr("href",
                    manifestPath);
            head.appendElement("meta").attr("name", "theme-color")
                    .attr("content", "#0b5fff");
            head.appendElement("meta").attr("name", "mobile-web-app-capable")
                    .attr("content", "yes");
            head.appendElement("meta")
                    .attr("name", "apple-mobile-web-app-capable")
                    .attr("content", "yes");
            head.appendElement("link").attr("rel", "apple-touch-icon")
                    .attr("href", icon192);
            head.appendElement("link").attr("rel", "icon")
                    .attr("sizes", "192x192").attr("href", icon192);
            head.appendElement("link").attr("rel", "icon")
                    .attr("sizes", "512x512").attr("href", icon512);

            head.appendElement("script")
                    .append("""
                            if ('serviceWorker' in navigator) {
                                window.addEventListener('load', function() {
                                    navigator.serviceWorker.register('%s')
                                        .catch(function(error) { console.error(error); });

                                    var returnUrlKey = 'pwa-return-url';
                                    var offlinePath = '%s/offline.html';
                                    var appPath = '%s/';

                                    window.addEventListener('offline', function() {
                                        if (window.location.pathname !== offlinePath) {
                                            try {
                                                sessionStorage.setItem(returnUrlKey,
                                                    window.location.href);
                                            } catch (ignore) {
                                                // No-op
                                            }
                                            window.location.href = offlinePath;
                                        }
                                    });

                                    window.addEventListener('online', function() {
                                        if (window.location.pathname === offlinePath) {
                                            var returnUrl = '';
                                            try {
                                                returnUrl = sessionStorage.getItem(returnUrlKey)
                                                    || '';
                                            } catch (ignore) {
                                                // No-op
                                            }
                                            if (returnUrl.indexOf(offlinePath) !== -1) {
                                                returnUrl = '';
                                            }
                                            window.location.href = returnUrl || appPath;
                                        }
                                    });

                                    if (!navigator.onLine
                                            && window.location.pathname !== offlinePath) {
                                        try {
                                            sessionStorage.setItem(returnUrlKey,
                                                window.location.href);
                                        } catch (ignore) {
                                            // No-op
                                        }
                                        window.location.href = offlinePath;
                                    }
                                });
                            }
                            """
                            .formatted(swPath, contextPath, contextPath));
        }

        @Override
        public void modifyBootstrapFragment(
                BootstrapFragmentResponse response) {
            // No-op: not needed in this case
        }
    }

    final AtomicInteger exceptionCount = new AtomicInteger(1);

    @Override
    protected DeploymentConfiguration createDeploymentConfiguration(
            Properties initParameters) {
        var env = System.getProperty("vaadin.productionMode");
        var productionMode = "false";
        if (env != null) {
            productionMode = env.equals("true") ? "true" : "false";
        }
        initParameters.put("productionMode", productionMode);
        return new DefaultDeploymentConfiguration(getClass(), initParameters);
    }

    @Override
    protected void servletInitialized() throws ServletException {
        super.servletInitialized();
        // Disable session expired notification and redirect to login view
        getService().setSystemMessagesProvider(systemMessagesInfo -> {
            CustomizedSystemMessages messages = new CustomizedSystemMessages();
            messages.setSessionExpiredNotificationEnabled(false);
            messages.setSessionExpiredURL(null);
            return messages;
        });

        // Add session init and destroy listeners
        getService().addSessionInitListener(event -> {
            logger.debug("Session started");
            VaadinSession session = event.getSession();
            session.getSession().setMaxInactiveInterval(300);
            session.addRequestHandler(this::handleRequest);
            // Set error handler for the session to login all exceptions
            // happening in the session and show them to the user in the UI
            session.setErrorHandler(
                    errorEvent -> handleError(errorEvent, session));
            session.addBootstrapListener(new LocaleBootstrapListener());
        });
        getService().addSessionDestroyListener(event -> {
            // The servlet container typically does not immediately
            // invalidate timed out sessions, so we need to do ourselves if
            // we want an eager cleanup.
            var wrappedSession = event.getSession().getSession();
            if (wrappedSession != null) {
                logger.info("Invalidating session");
                try {
                    wrappedSession.invalidate();
                } catch (IllegalStateException e) {
                    logger.warn("Session already invalidated");
                }
            }
            logger.debug("Session ended");
        });
        getService().addServiceDestroyListener(
                serviceDestroy -> EventBus.get().shutdown());
    }

    protected void handleError(ErrorEvent errorEvent, VaadinSession session) {
        var throwable = errorEvent.getThrowable();
        // It is Jetty vs. Firefox issue, hence not showing to user
        // https://github.com/jetty/jetty.project/issues/9763
        if (!(throwable.toString()
                .contains("org.eclipse.jetty.io.EofException"))) {
            String id = formatId();
            String message;
            // Handle DatabaseConnectionException separately
            if (Utils.throwableHasCause(throwable,
                    DatabaseConnectionException.class)) {
                message = "Database connection error.";
            } else {
                message = Utils.getRootCause(throwable).getMessage();
            }
            session.getUIs().forEach(ui -> ui.access(() -> ((VaadinCreateUI) ui)
                    .showInternalError(message, id)));
            logger.error("Exception happened {}", id,
                    errorEvent.getThrowable());
            Telemetry.exception(errorEvent.getThrowable());
        }
    }

    private String formatId() {
        return String
                .format("#%10s", String.valueOf(exceptionCount.getAndAdd(1)))
                .replace(' ', '0');
    }

    // Use request handler to persist the selected language to Cookie
    protected boolean handleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) {
        var locale = new StringBuilder();
        Cookie localeCookie = CookieUtils
                .getCookieByName(CookieUtils.COOKIE_LANGUAGE, request);
        session.accessSynchronously(() -> {
            var l = (String) session.getAttribute("locale");
            if (l != null) {
                logger.debug("Found locale in session: {}", l);
                locale.append(l);
            }
        });
        if (!locale.isEmpty()) {
            boolean toSave = false;
            if (localeCookie == null) {
                localeCookie = CookieUtils.createNewCookie(request,
                        locale.toString());
                toSave = true;
            } else {
                var newValue = locale.toString();
                var oldValue = localeCookie.getValue();
                if (!newValue.equals(oldValue)) {
                    localeCookie.setValue(locale.toString());
                    toSave = true;
                }
            }
            if (toSave) {
                logger.info("Saving language '{}' in cookie", locale);
                response.addCookie(localeCookie);
                ((VaadinServletResponse) response)
                        .setLocale(Locale.forLanguageTag(locale.toString()));
            }
        }
        return false;
    }

    private static final Logger logger = LoggerFactory.getLogger(Servlet.class);
}
