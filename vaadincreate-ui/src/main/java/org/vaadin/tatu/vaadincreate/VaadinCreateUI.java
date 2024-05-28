package org.vaadin.tatu.vaadincreate;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.admin.AdminView;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.auth.BasicAccessControl;
import org.vaadin.tatu.vaadincreate.backend.AppDataService;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.UserService;
import org.vaadin.tatu.vaadincreate.backend.data.Message;
import org.vaadin.tatu.vaadincreate.crud.BooksView;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.login.LoginView;
import org.vaadin.tatu.vaadincreate.stats.StatsView;
import org.vaadin.tatu.vaadincreate.util.CookieUtil;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Viewport;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

@Theme("vaadincreate")
@StyleSheet("vaadin://styles/additional-styles.css")
@SuppressWarnings("serial")
@Push(transport = Transport.WEBSOCKET_XHR)
@PreserveOnRefresh
@Viewport("width=device-width, initial-scale=1, maximum-scale=1.0, user-scalable=no")
public class VaadinCreateUI extends UI implements EventBusListener, HasI18N {

    // Inject the default services. This demo application does not include real
    // database backend, only a mock, which is used here. This works well also
    // in the unit tests included. Supposed these were the real production
    // services this UI can be extended and getters for the services overriden
    // for creating test UI for unit tests.
    private AccessControl accessControl = new BasicAccessControl();
    private ProductDataService productService = ProductDataService.get();
    private UserService userService = UserService.get();
    private AppDataService appService = AppDataService.get();

    private EventBus eventBus = EventBus.get();
    private String target;

    @Override
    protected void init(VaadinRequest request) {
        getPage().setTitle("Vaadin Create 23'");
        if (!getAccessControl().isUserSignedIn()) {
            showLoginView(request);
        } else {
            target = getInitialTarget();
            showAppLayout();
        }
        eventBus.registerEventBusListener(this);
    }

    private void showLoginView(VaadinRequest request) {
        setContent(new LoginView(getAccessControl(), e -> onLogin()));
    }

    private void onLogin() {
        target = getInitialTarget();
        logger.info("Initial target '{}'", target);
        Utils.sessionFixation();
        getPage().reload();
        showAppLayout();
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

        getNavigator().navigateTo(target);
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
        return accessControl;
    }

    /**
     * Get ProductDataService in use
     *
     * @return Instance of ProductDataService
     */
    public ProductDataService getProductService() {
        return productService;
    }

    /**
     * Get UserService in use
     *
     * @return Instance of UserService
     */
    public UserService getUserService() {
        return userService;
    }

    /**
     * Get AppDataService in use
     *
     * @return Instance of AppAdataService
     */
    public AppDataService getAppService() {
        return appService;
    }

    @Override
    public void eventFired(Object event) {
        if (event instanceof Message) {
            Message message = (Message) event;

            access(() -> {
                var note = new Notification(message.getDateStamp().toString(),
                        message.getMessage(), Type.TRAY_NOTIFICATION, true);
                note.show(getPage());
            });
        }
    }

    @Override
    public void detach() {
        super.detach();
        eventBus.unregisterEventBusListener(this);
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    // Set maxIdleTime because of Jetty 10, see:
    // https://github.com/vaadin/flow/issues/17215
    @WebServlet(value = "/*", asyncSupported = true, initParams = {
            @WebInitParam(name = "org.atmosphere.websocket.maxIdleTime", value = "300000") })
    @VaadinServletConfiguration(productionMode = false, ui = VaadinCreateUI.class)
    public static class Servlet extends VaadinServlet {

        @Override
        protected void servletInitialized() {
            getService().addSessionInitListener(event -> {
                VaadinSession s = event.getSession();
                s.addRequestHandler((session, request,
                        response) -> handleRequest(session, request, response));
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

        private Logger logger = LoggerFactory.getLogger(this.getClass());
    }

}
