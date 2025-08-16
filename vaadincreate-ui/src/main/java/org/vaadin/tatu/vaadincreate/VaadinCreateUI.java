package org.vaadin.tatu.vaadincreate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.events.AbstractEvent;
import org.vaadin.tatu.vaadincreate.backend.events.MessageEvent;
import org.vaadin.tatu.vaadincreate.backend.events.ShutdownEvent;
import org.vaadin.tatu.vaadincreate.backend.events.UserUpdatedEvent;
import org.vaadin.tatu.vaadincreate.crud.BooksView;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus;
import org.vaadin.tatu.vaadincreate.eventbus.EventBus.EventBusListener;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.login.LoginView;
import org.vaadin.tatu.vaadincreate.stats.StatsView;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Viewport;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

@NullMarked
@Theme("vaadincreate")
@StyleSheet("vaadin://styles/additional-styles.css")
@SuppressWarnings({ "serial", "java:S2160" })
@Push(transport = Transport.WEBSOCKET_XHR)
@Viewport("width=device-width, initial-scale=1, maximum-scale=5.0, user-scalable=yes")
public class VaadinCreateUI extends UI implements EventBusListener, HasI18N {

    // Inject the default services. This demo application does not include real
    // database backend, only a mock, which is used here. This works well also
    // in the unit tests included. Supposed these were the real production
    // services this UI can be extended and getters for the services overriden
    // for creating test UI for unit tests.
    @Nullable
    private transient ProductDataService productService = ProductDataService
            .get();

    @Nullable
    private transient UserService userService = UserService.get();

    @Nullable
    private transient AppDataService appService = AppDataService.get();

    @Nullable
    private transient ExecutorService executor;

    @Nullable
    private String target;

    @Override
    protected void init(VaadinRequest request) {
        setTabIndex(0);
        setOverlayContainerLabel("");
        getNotificationConfiguration()
                .setAssistivePostfix(Type.ASSISTIVE_NOTIFICATION, "");
        getNotificationConfiguration()
                .setAssistivePrefix(Type.ASSISTIVE_NOTIFICATION, "");
        getPage().setTitle("Vaadin Create 23'");
        if (!getAccessControl().isUserSignedIn()) {
            MDC.clear();
            showLoginView();
        } else {
            var id = Utils.getCurrentUserOrThrow().getId();
            assert id != null : "User id must not be null";
            var user = getUserService().getUserById(id);
            CurrentUser.set(user);
            target = getInitialTarget();
            showAppLayout();
        }
    }

    private void showLoginView() {
        setContent(new LoginView(getAccessControl(), login -> onLogin()));
    }

    private void onLogin() {
        target = getInitialTarget();
        logger.info("Initial target '{}'", target);
        // Normally we should do session fixation here, but this is not
        // working with Nginx community edition.
        getPage().reload();
        showAppLayout();
    }

    public void showInternalError(String message, String id) {
        var failure = Notification.show(getTranslation(I18n.EXCEPTION, id),
                message, Type.ERROR_MESSAGE);
        failure.setStyleName(ValoTheme.NOTIFICATION_FAILURE);
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
        getEventBus().registerEventBusListener(this);

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
                .findDraft(Utils.getCurrentUserOrThrow());
        if (draft != null && getAccessControl().isUserInRole(User.Role.ADMIN)) {
            handleDraft(draft);
        }
        if (target.isEmpty()) {
            logger.info("Reroute to about view");
        }
    }

    // Handle draft product found in the database and ask user what to do with
    // it.
    private void handleDraft(Product draft) {
        getNavigator().navigateTo(AboutView.VIEW_NAME);
        logger.info("Draft found");
        var dialog = new ConfirmDialog(getTranslation(I18n.CONFIRM),
                getTranslation(I18n.DRAFT_FOUND), ConfirmDialog.Type.ALERT);
        dialog.setCancelText(getTranslation(I18n.DISCARD));
        dialog.setConfirmText(getTranslation(I18n.YES));
        var id = draft.getId() == null ? "new" : String.valueOf(draft.getId());
        dialog.addConfirmedListener(confirmed -> getNavigator()
                .navigateTo(String.format("%s/%s", BooksView.VIEW_NAME, id)));
        dialog.addCancelledListener(cancelled -> {
            logger.info("Draft discarded");
            getProductService().saveDraft(CurrentUser.get().get(), null);
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
    public void eventFired(AbstractEvent event) {
        if (event instanceof MessageEvent message) {
            access(() -> {
                var note = new Notification(
                        Utils.formatDate(message.timeStamp(), getLocale()),
                        message.message(), Type.TRAY_NOTIFICATION, true);
                note.show(getPage());
            });
        }
        if (event instanceof UserUpdatedEvent userUpdated) {
            var user = (User) getSession().getSession().getAttribute(
                    CurrentUser.CURRENT_USER_SESSION_ATTRIBUTE_KEY);
            assert user != null : "User must not be null";
            var userId = user.getId();
            assert userId != null : "User id must not be null";
            if (userId.equals(userUpdated.userId())) {
                logger.debug("User was updated, updating CurrentUser");
                var updatedUser = getUserService().getUserById(userId);
                access(() -> getSession().getSession().setAttribute(
                        CurrentUser.CURRENT_USER_SESSION_ATTRIBUTE_KEY,
                        updatedUser));
            }
        }
        if (event instanceof ShutdownEvent) {
            access(() -> Notification.show(getTranslation(I18n.LOGOUT_60S),
                    Type.WARNING_MESSAGE));
            CompletableFuture.runAsync(() -> access(() -> {
                if (getSession().getState() == VaadinSession.State.OPEN) {
                    logger.info("Performing scheduled logout");
                    getSession().close();
                    getPage().reload();
                }
            }), CompletableFuture.delayedExecutor(60, TimeUnit.SECONDS,
                    getExecutor()));
        }

    }

    @Override
    public void detach() {
        super.detach();
        // Unregister this UI instance from the event bus when it is detached
        getEventBus().unregisterEventBusListener(this);
        shutdownExecutor();
    }

    @Override
    public void attach() {
        super.attach();
        getPage().getJavaScript().execute(
                """
                        setTimeout(() => {
                            document.getElementsByClassName('v-tooltip')[0].style.zIndex=30001;
                        }, 200);
                        """);
    }

    /**
     * Retrieves the singleton instance of the EventBus.
     * 
     * @return the EventBus instance
     */
    private EventBus getEventBus() {
        return EventBus.get();
    }

    /**
     * Retrieves the ExecutorService instance, initializing it if necessary. The
     * executor is used to run background tasks.
     * 
     * @return the ExecutorService instance
     */
    public ExecutorService getExecutor() {
        if (executor == null) {
            logger.info("Creating executor");
            executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                if (!executor.isShutdown()) {
                    // Add user id to MDC for logging
                    logger.debug("Adding user id to MDC");
                    var user = (User) getSession().getSession().getAttribute(
                            CurrentUser.CURRENT_USER_SESSION_ATTRIBUTE_KEY);
                    if (user != null) {
                        var userId = String.format("[%s/%s]",
                                user.getRole().toString(), user.getName());
                        MDC.put("userId", userId);
                    }
                }
            });
        }
        return executor;
    }

    private void shutdownExecutor() {
        if (executor != null) {
            logger.info("Shutting down executor");
            executor.shutdown();
            executor = null;
        }
    }

    private static Logger logger = LoggerFactory
            .getLogger(VaadinCreateUI.class);


}
