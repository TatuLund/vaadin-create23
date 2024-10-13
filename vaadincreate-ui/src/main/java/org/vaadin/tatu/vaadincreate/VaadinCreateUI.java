package org.vaadin.tatu.vaadincreate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;

import org.eclipse.jetty.io.EofException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.vaadin.tatu.vaadincreate.admin.AdminView;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.auth.BasicAccessControl;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.backend.AppDataService;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.UserService;
import org.vaadin.tatu.vaadincreate.backend.data.Message;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.crud.BooksView;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.login.LoginView;
import org.vaadin.tatu.vaadincreate.stats.StatsView;
import org.vaadin.tatu.vaadincreate.util.CookieUtil;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Viewport;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.CustomizedSystemMessages;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

@Theme("vaadincreate")
@StyleSheet("vaadin://styles/additional-styles.css")
@SuppressWarnings({ "serial", "java:S2160" })
@Push(transport = Transport.WEBSOCKET_XHR)
@Viewport("width=device-width, initial-scale=1, maximum-scale=1.0, user-scalable=no")
public class VaadinCreateUI extends UI implements EventBusListener, HasI18N {

    // Inject the default services. This demo application does not include real
    // database backend, only a mock, which is used here. This works well also
    // in the unit tests included. Supposed these were the real production
    // services this UI can be extended and getters for the services overriden
    // for creating test UI for unit tests.
    private transient ProductDataService productService = ProductDataService
            .get();
    private transient UserService userService = UserService.get();
    private transient AppDataService appService = AppDataService.get();
    private transient ExecutorService executor;

    private String target;

    @Override
    protected void init(VaadinRequest request) {
        getPage().setTitle("Vaadin Create 23'");
        if (!getAccessControl().isUserSignedIn()) {
            showLoginView();
        } else {
            target = getInitialTarget();
            showAppLayout();
        }
        getEventBus().registerEventBusListener(this);
    }

    private void showLoginView() {
        setContent(new LoginView(getAccessControl(), e -> onLogin()));
    }

    private void onLogin() {
        target = getInitialTarget();
        logger.info("Initial target '{}'", target);
        Utils.sessionFixation();
        getPage().reload();
        showAppLayout();
    }

    public void showInternalError(String message) {
        Notification.show(getTranslation(I18n.EXCEPTION, message),
                Type.ERROR_MESSAGE);
    }

    private String getInitialTarget() {
        if (getPage().getUriFragment() == null) {
            return AboutView.VIEW_NAME;
        }
        var location = getPage().getLocation().toString();
        var index = location.indexOf("#!") + 2;
        return location.substring(index);
    }

    protected void showAppLayout() {
        var appLayout = new AppLayout(this, getAccessControl());
        setContent(appLayout);

        // Use String constants for view names, allows easy refactoring if so
        // needed
        appLayout.addView(AboutView.class, getTranslation(AboutView.VIEW_NAME),
                VaadinIcons.INFO, AboutView.VIEW_NAME);
        appLayout.addView(BooksView.class, getTranslation(BooksView.VIEW_NAME),
                VaadinIcons.TABLE, BooksView.VIEW_NAME);
        appLayout.addView(StatsView.class, getTranslation(StatsView.VIEW_NAME),
                VaadinIcons.CHART, StatsView.VIEW_NAME);
        appLayout.addView(AdminView.class, getTranslation(AdminView.VIEW_NAME),
                VaadinIcons.USERS, AdminView.VIEW_NAME);

        Product draft = getProductService()
                .findDraft(getAccessControl().getPrincipalName());
        if (draft != null) {
            handleDraft(draft);
        }
        if (target.isEmpty()) {
            logger.info("Reroute to about view");
        }
    }

    // Handle draft product found in the database and ask user what to do with
    // it.
    private void handleDraft(Product draft) {
        logger.info("Draft found");
        var dialog = new ConfirmDialog(getTranslation(I18n.DRAFT_FOUND),
                ConfirmDialog.Type.ALERT);
        dialog.setCancelText(getTranslation(I18n.DISCARD));
        dialog.setConfirmText(getTranslation(I18n.YES));
        var id = draft.getId() == null ? "new" : String.valueOf(draft.getId());
        dialog.addConfirmedListener(e -> getNavigator()
                .navigateTo(String.format("%s/%s", BooksView.VIEW_NAME, id)));
        dialog.addCancelListener(e -> {
            logger.info("Draft discarded");
            getProductService().saveDraft(getAccessControl().getPrincipalName(),
                    null);
            getNavigator().navigateTo(target);
        });
        dialog.open();
    }

    public static VaadinCreateUI get() {
        return (VaadinCreateUI) UI.getCurrent();
    }

    /**
     * Get AccessControl in use
     *
     * @return Instance of AccessControl
     */
    public AccessControl getAccessControl() {
        var accessControl = getSession().getAttribute(AccessControl.class);
        if (accessControl == null) {
            accessControl = new BasicAccessControl();
            getSession().setAttribute(AccessControl.class, accessControl);
        }
        return accessControl;
    }

    /**
     * Get ProductDataService in use
     *
     * @return Instance of ProductDataService
     */
    public ProductDataService getProductService() {
        if (productService == null) {
            productService = ProductDataService.get();
        }
        return productService;
    }

    /**
     * Get UserService in use
     *
     * @return Instance of UserService
     */
    public UserService getUserService() {
        if (userService == null) {
            userService = UserService.get();
        }
        return userService;
    }

    /**
     * Get AppDataService in use
     *
     * @return Instance of AppAdataService
     */
    public AppDataService getAppService() {
        if (appService == null) {
            appService = AppDataService.get();
        }
        return appService;
    }

    @Override
    public void eventFired(Object event) {
        if (event instanceof Message) {
            Message message = (Message) event;

            access(() -> {
                var note = new Notification(
                        Utils.formatDate(message.getDateStamp(), getLocale()),
                        message.getMessage(), Type.TRAY_NOTIFICATION, true);
                note.show(getPage());
            });
        }
    }

    @Override
    public void detach() {
        super.detach();
        getEventBus().unregisterEventBusListener(this);
    }

    private EventBus getEventBus() {
        return EventBus.get();
    }

    public ExecutorService getExecutor() {
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                // Add user id to MDC for logging
                var user = (User) getSession().getSession().getAttribute(
                        CurrentUser.CURRENT_USER_SESSION_ATTRIBUTE_KEY);
                if (user != null) {
                    var userId = String.format("[%s/%s]",
                            user.getRole().toString(), user.getName());
                    MDC.put("userId", userId);
                }
            });
        }
        return executor;
    }

    private static Logger logger = LoggerFactory
            .getLogger(VaadinCreateUI.class);

    // Set maxIdleTime because of Jetty 10, see:
    // https://github.com/vaadin/flow/issues/17215
    @WebServlet(value = "/*", asyncSupported = true, initParams = {
            @WebInitParam(name = "org.atmosphere.cpr.AtmosphereConfig.getInitParameter", value = "true"),
            @WebInitParam(name = "org.atmosphere.websocket.maxIdleTime", value = "300000") })
    @VaadinServletConfiguration(productionMode = false, ui = VaadinCreateUI.class, heartbeatInterval = 60, closeIdleSessions = true)
    public static class Servlet extends VaadinServlet {

        @Override
        protected void servletInitialized() {
            // Disable session expired notification and redirect to login view
            getService().setSystemMessagesProvider(systemMessagesInfo -> {
                CustomizedSystemMessages messages = new CustomizedSystemMessages();
                messages.setSessionExpiredNotificationEnabled(false);
                messages.setSessionExpiredURL(null);
                return messages;
            });

            // Add session init and destroy listeners
            getService().addSessionInitListener(event -> {
                logger.info("Session started");
                VaadinSession session = event.getSession();
                session.getSession().setMaxInactiveInterval(300);
                session.addRequestHandler(this::handleRequest);
                session.setAttribute(WrappedSession.class,
                        session.getSession());
                // Set error handler for the session to login all exceptions
                // happening in the session and show them to the user in the UI
                session.setErrorHandler(errorHandler -> {
                    var throwable = errorHandler.getThrowable();
                    // It is Jetty vs. Firefox issue, hence not showing to user
                    // https://github.com/jetty/jetty.project/issues/9763
                    if (!(throwable instanceof EofException)) {
                        var message = throwable.getLocalizedMessage();
                        session.getUIs().forEach(
                                ui -> ui.access(() -> ((VaadinCreateUI) ui)
                                        .showInternalError(message)));
                        logger.error("Exception happened",
                                errorHandler.getThrowable());
                    }
                });
            });
            getService().addSessionDestroyListener(event -> {
                // The servlet container typically does not immediately
                // invalidate timed out sessions, so we need to do ourselves if
                // we want an eager cleanup.
                var wrappedSession = event.getSession()
                        .getAttribute(WrappedSession.class);
                if (wrappedSession != null) {
                    logger.info("Invalidating session");
                    wrappedSession.invalidate();
                }
                logger.info("Session eneded");
            });
        }

        // Use request handler to persist the selected language to Cookie
        private boolean handleRequest(VaadinSession session,
                VaadinRequest request, VaadinResponse response) {
            var locale = new StringBuilder();
            Cookie localeCookie = CookieUtil.getCookieByName("language",
                    request);
            session.accessSynchronously(() -> {
                var l = (String) session.getAttribute("locale");
                if (l != null) {
                    locale.append(l);
                }
            });
            if (locale.length() != 0) {
                boolean toSave = false;
                if (localeCookie == null) {
                    localeCookie = createNewCookie(request, locale);
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
                }
            }
            return false;
        }

        private Cookie createNewCookie(VaadinRequest request,
                StringBuilder locale) {
            Cookie localeCookie;
            localeCookie = new Cookie("language", locale.toString());
            localeCookie.setPath(request.getContextPath());
            localeCookie.setMaxAge(60 * 60);
            return localeCookie;
        }

        private final Logger logger = LoggerFactory.getLogger(this.getClass());
    }

}
