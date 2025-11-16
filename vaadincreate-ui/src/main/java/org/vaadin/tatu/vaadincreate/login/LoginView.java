package org.vaadin.tatu.vaadincreate.login;

import java.lang.reflect.Method;
import java.util.Locale;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension;
import org.vaadin.tatu.vaadincreate.components.CapsLockWarning;
import org.vaadin.tatu.vaadincreate.components.Html;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18NProvider;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.event.ConnectorEventListener;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.Page;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.util.ReflectTools;

/**
 * UI content when the user is not logged in yet.
 */
@SuppressWarnings({ "serial", "java:S2160" })
@NullMarked
public class LoginView extends Composite implements HasI18N {

    private static final Logger logger = LoggerFactory
            .getLogger(LoginView.class);

    // Package-private final fields
    final TextField usernameField = new TextField(
            getTranslation(I18n.USERNAME));
    final PasswordField passwordField = new PasswordField(
            getTranslation(I18n.PASSWORD));
    final Button login = new Button(getTranslation(I18n.Login.LOGIN_BUTTON));

    // Private final fields
    private final CssLayout layout = new CssLayout();
    private final AccessControl accessControl;
    private final Button forgotPassword = new Button(
            getTranslation(I18n.Login.FORGOT_PASSWORD));
    private final CssLayout loginInformation;
    private final ComboBox<Locale> lang = new LanguageSelect();
    private final Label loginInfoText = new Label(getLoginInfoText(),
            ContentMode.HTML);
    private final CapsLockWarning capsLockWarning = CapsLockWarning
            .warnFor(passwordField);

    // Private mutable fields
    @Nullable
    private Registration resizeReg;

    /**
     * Constructs the LoginView.
     *
     * @param accessControl
     *            the access control implementation used for authentication
     * @param loginListener
     *            the listener to handle successful login events
     */
    public LoginView(AccessControl accessControl, LoginListener loginListener) {
        this.accessControl = accessControl;
        // information text about logging in
        loginInformation = buildLoginInformation();
        addLoginListener(loginListener);
        setCompositionRoot(layout);
    }

    void buildUI() {
        layout.addStyleName(VaadinCreateTheme.LOGINVIEW);

        // login form, centered in the available part of the screen
        var loginForm = buildLoginForm();

        // layout to center login form when there is sufficient screen space
        // - see the theme for how this is made responsive for various screen
        // sizes
        var centeringLayout = new VerticalLayout();
        centeringLayout.setMargin(false);
        centeringLayout.setSpacing(false);
        centeringLayout.setStyleName(VaadinCreateTheme.LOGINVIEW_CENTER);
        centeringLayout.addComponent(loginForm);
        centeringLayout.setComponentAlignment(loginForm,
                Alignment.MIDDLE_CENTER);

        layout.addComponent(centeringLayout);
    }

    @Override
    public void attach() {
        super.attach();
        resizeReg = getUI().getPage().addBrowserWindowResizeListener(
                windowResize -> showLoginInformation(windowResize.getWidth()));
        buildUI();
        showLoginInformation(getUI().getPage().getBrowserWindowWidth());
        usernameField.focus();
        Utils.startPolling();
    }

    private void updateTranslations() {
        if (getUI().getPage().getBrowserWindowWidth() < 800) {
            usernameField.setPlaceholder(getTranslation(I18n.USERNAME));
            passwordField.setPlaceholder(getTranslation(I18n.PASSWORD));
        } else {
            usernameField.setCaption(getTranslation(I18n.USERNAME));
            passwordField.setCaption(getTranslation(I18n.PASSWORD));
            lang.setCaption(getTranslation(I18n.Login.LANGUAGE));
        }
        login.setCaption(getTranslation(I18n.Login.LOGIN_BUTTON));
        forgotPassword.setCaption(getTranslation(I18n.Login.FORGOT_PASSWORD));
        loginInfoText.setValue(getLoginInfoText());
        capsLockWarning.setMessage(getTranslation(I18n.Login.CAPSLOCK));
    }

    private String getLoginInfoText() {
        // Build heading and supporting text using Html builder (escaped by
        // default)
        var heading = Html.h1().text(getTranslation(I18n.Login.LOGIN_INFO))
                .build();
        var supporting = Html.span()
                .text(getTranslation(I18n.Login.LOGIN_INFO_TEXT)).build();
        // Preserve original spacing pattern between heading and text
        return heading + " " + supporting;
    }

    @Override
    public void detach() {
        super.detach();
        Utils.stopPolling();
        resizeReg.remove();
    }

    private void showLoginInformation(int width) {
        if (width < 1000 && width >= 800) {
            layout.removeComponent(loginInformation);
            wideLogin();
        } else if (width < 800) {
            layout.removeComponent(loginInformation);
            narrowLogin();
        } else {
            layout.addComponent(loginInformation);
            wideLogin();
        }
    }

    private void narrowLogin() {
        usernameField.setPlaceholder(getTranslation(I18n.USERNAME));
        passwordField.setPlaceholder(getTranslation(I18n.PASSWORD));
        usernameField.setCaption("");
        passwordField.setCaption("");
        lang.setCaption("");
    }

    private void wideLogin() {
        usernameField.setPlaceholder("");
        passwordField.setPlaceholder("");
        usernameField.setCaption(getTranslation(I18n.USERNAME));
        passwordField.setCaption(getTranslation(I18n.PASSWORD));
        lang.setCaption(getTranslation(I18n.Login.LANGUAGE));
    }

    private Component buildLoginForm() {
        var loginForm = new FormLayout();

        loginForm.addStyleName(VaadinCreateTheme.LOGINVIEW_FORM);
        loginForm.setSizeUndefined();
        loginForm.setMargin(false);
        AttributeExtension.of(loginForm).setAttribute(AriaAttributes.ROLE,
                AriaRoles.FORM);
        AttributeExtension.of(loginForm).setAttribute(AriaAttributes.LABEL,
                getTranslation(I18n.Login.LOGIN_BUTTON));

        loginForm.addComponent(usernameField);
        usernameField.setWidth(18, Unit.EM);
        usernameField.setId("login-username-field");

        loginForm.addComponent(passwordField);
        capsLockWarning.setMessage(getTranslation(I18n.Login.CAPSLOCK));
        passwordField.setWidth(18, Unit.EM);
        passwordField.setId("login-password-field");
        CssLayout buttons = new CssLayout();
        buttons.setStyleName("buttons");
        loginForm.addComponent(buttons);

        buttons.addComponent(login);
        login.setDisableOnClick(true);
        login.setId("login-button");
        login.addClickListener(click -> {
            try {
                login();
            } finally {
                login.setEnabled(true);
            }
        });
        login.setClickShortcut(ShortcutAction.KeyCode.ENTER);
        login.addStyleName(ValoTheme.BUTTON_FRIENDLY);

        forgotPassword.addStyleNames(ValoTheme.BUTTON_LINK,
                VaadinCreateTheme.LOGINVIEW_FORGOTBUTTON);
        buttons.addComponent(forgotPassword);
        forgotPassword.addClickListener(click -> showHintNotification());

        lang.setWidth(18, Unit.EM);
        loginForm.addComponent(lang);
        lang.addValueChangeListener(valueChange -> {
            var ui = getUI();
            // Set the locale to session attribute, request handler will persist
            // it to the cookie from there.
            ui.getSession().setAttribute("locale",
                    valueChange.getValue().getLanguage());
            Locale.setDefault(valueChange.getValue());
            ui.getSession().setLocale(valueChange.getValue());
            updateTranslations();
            logger.info("Changing locale to {}",
                    valueChange.getValue().getLanguage());
        });
        var locale = I18NProvider.fetchLocaleFromCookie();
        lang.setValue(locale);

        return loginForm;
    }

    private CssLayout buildLoginInformation() {
        var infoLayout = new CssLayout();
        infoLayout.setId("info-layout");
        var resource = new ThemeResource("images/bookstore.png");
        var image = new Image("", resource);
        image.setAlternateText("Bookstore logo");
        image.setWidthFull();
        infoLayout.setStyleName(VaadinCreateTheme.LOGINVIEW_INFORMATION);
        loginInfoText.setWidthFull();
        infoLayout.addComponents(image, loginInfoText);
        return infoLayout;
    }

    // Attempt to log in the user and fire an event to notify listeners.
    private void login() {
        if (accessControl.signIn(usernameField.getValue(),
                passwordField.getValue())) {
            fireEvent(new LoginEvent(this));
        } else {
            showLoginFailedNotification();
            usernameField.focus();
        }
    }

    private void showHintNotification() {
        var notification = new Notification(getTranslation(I18n.Login.HINT));
        // keep the notification visible a little while after moving the mouse,
        // or until clicked
        notification.setDelayMsec(2000);
        notification.show(Page.getCurrent());
    }

    private void showLoginFailedNotification() {
        var notification = new Notification(
                getTranslation(I18n.Login.LOGIN_FAILED),
                getTranslation(I18n.Login.LOGIN_FAILED_DESC),
                Notification.Type.HUMANIZED_MESSAGE);
        // keep the notification visible a little while after moving the mouse,
        // or until clicked
        notification.setDelayMsec(2000);
        notification.show(Page.getCurrent());
    }

    /**
     * Add event listener for login event. Event is fired when user logged in.
     *
     * @param listener
     *            The listener, can be Lambda expression.
     * @return Registration Use Registration#remove() for listener removal.
     */
    public Registration addLoginListener(LoginListener listener) {
        return addListener(LoginEvent.class, listener,
                LoginListener.LOGIN_METHOD);
    }

    /**
     * Login listener interface, can be implemented with Lambda or anonymous
     * inner class.
     */
    public interface LoginListener extends ConnectorEventListener {
        Method LOGIN_METHOD = ReflectTools.findMethod(LoginListener.class,
                "login", LoginEvent.class);

        public void login(LoginEvent event);
    }

    /**
     * LoginEvent is fired when user logs in.
     */
    public static class LoginEvent extends Component.Event {

        public LoginEvent(Component source) {
            super(source);
        }
    }
}
