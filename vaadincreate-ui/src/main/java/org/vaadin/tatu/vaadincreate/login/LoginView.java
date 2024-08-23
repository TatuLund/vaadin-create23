package org.vaadin.tatu.vaadincreate.login;

import java.lang.reflect.Method;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.CapsLockWarning;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.auth.AccessControl;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18NProvider;

import com.vaadin.event.ConnectorEventListener;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.Page;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.FormLayout;
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
public class LoginView extends CssLayout implements HasI18N {

    private static final String LOGIN_FAILED = "login-failed";
    private static final String LOGIN_FAILED_DESC = "login-failed-desc";
    private static final String LOGIN_INFO = "login-info";
    private static final String LOGIN_INFO_TEXT = "login-info-text";
    private static final String LOGIN_BUTTON = "login-button";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String FORGOT_PASSWORD = "forgot-password";
    private static final String HINT = "hint";
    private static final String LANGUAGE = "language";
    private static final String CAPSLOCK = "capslock";

    TextField usernameField;
    PasswordField passwordField;
    Button login;
    private Button forgotPassword;
    private AccessControl accessControl;
    private Registration resizeReg;
    private CssLayout loginInformation;
    private ComboBox<Locale> lang;
    private Label loginInfoText;
    private CapsLockWarning capsLockWarning;

    public LoginView(AccessControl accessControl, LoginListener loginListener) {
        this.accessControl = accessControl;
        addLoginListener(loginListener);
    }

    void buildUI() {
        addStyleName(VaadinCreateTheme.LOGINVIEW);

        // information text about logging in
        loginInformation = buildLoginInformation();

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

        addComponent(centeringLayout);
    }

    @Override
    public void attach() {
        super.attach();
        resizeReg = getUI().getPage().addBrowserWindowResizeListener(
                e -> showLoginInformation(e.getWidth()));
        buildUI();
        usernameField.focus();
    }

    private void updateTranslations() {
        usernameField.setCaption(getTranslation(USERNAME));
        passwordField.setCaption(getTranslation(PASSWORD));
        login.setCaption(getTranslation(LOGIN_BUTTON));
        forgotPassword.setCaption(getTranslation(FORGOT_PASSWORD));
        loginInfoText.setValue(getLoginInfoText());
        lang.setCaption(getTranslation(LANGUAGE));
        capsLockWarning.setMessage(getTranslation(CAPSLOCK));
    }

    private String getLoginInfoText() {
        var h1 = createH1(getTranslation(LOGIN_INFO));
        return String.format("%s %s", h1, getTranslation(LOGIN_INFO_TEXT));
    }

    private static String createH1(String text) {
        return String.format("<h1>%s</h1>", text);
    }

    @Override
    public void detach() {
        super.detach();
        resizeReg.remove();
    }

    private void showLoginInformation(int width) {
        if (width < 700) {
            removeComponent(loginInformation);
        } else {
            addComponent(loginInformation);
        }
    }

    private Component buildLoginForm() {
        var loginForm = new FormLayout();

        loginForm.addStyleName(VaadinCreateTheme.LOGINVIEW_FORM);
        loginForm.setSizeUndefined();
        loginForm.setMargin(false);

        loginForm.addComponent(usernameField = new TextField(
                getTranslation(USERNAME), "Admin"));
        usernameField.setWidth(15, Unit.EM);
        usernameField.setId("login-username-field");
        loginForm.addComponent(
                passwordField = new PasswordField(getTranslation(PASSWORD)));
        capsLockWarning = CapsLockWarning.warnFor(passwordField);
        capsLockWarning.setMessage(getTranslation(CAPSLOCK));
        passwordField.setWidth(15, Unit.EM);
        passwordField.setDescription(getTranslation(HINT));
        passwordField.setId("login-password-field");
        CssLayout buttons = new CssLayout();
        buttons.setStyleName("buttons");
        loginForm.addComponent(buttons);

        buttons.addComponent(login = new Button(getTranslation(LOGIN_BUTTON)));
        login.setDisableOnClick(true);
        login.setId("login-button");
        login.addClickListener(event -> {
            try {
                login();
            } finally {
                login.setEnabled(true);
            }
        });
        login.setClickShortcut(ShortcutAction.KeyCode.ENTER);
        login.addStyleName(ValoTheme.BUTTON_FRIENDLY);

        forgotPassword = new Button(getTranslation(FORGOT_PASSWORD));
        buttons.addComponent(forgotPassword);
        forgotPassword.addClickListener(event -> showNotification(
                new Notification(getTranslation(HINT))));
        forgotPassword.addStyleName(ValoTheme.BUTTON_LINK);

        lang = new LanguageSelect();
        loginForm.addComponent(lang);
        lang.addValueChangeListener(e -> {
            var ui = getUI();
            // Set the locale to session attribute, request handler will persist
            // it to the cookie from there.
            ui.getSession().setAttribute("locale", e.getValue().getLanguage());
            Locale.setDefault(e.getValue());
            ui.getSession().setLocale(e.getValue());
            updateTranslations();
            logger.info("Changing locale to {}", e.getValue().getLanguage());
        });
        var locale = I18NProvider.fetchLocaleFromCookie();
        lang.setValue(locale);

        return loginForm;
    }

    private CssLayout buildLoginInformation() {
        var layout = new CssLayout();
        layout.setStyleName(VaadinCreateTheme.LOGINVIEW_INFORMATION);
        loginInfoText = new Label(getLoginInfoText(), ContentMode.HTML);
        loginInfoText.setSizeFull();
        layout.addComponent(loginInfoText);
        return layout;
    }

    // Attempt to log in the user and fire an event to notify listeners.
    private void login() {
        if (accessControl.signIn(usernameField.getValue(),
                passwordField.getValue())) {
            fireEvent(new LoginEvent(this));
        } else {
            showNotification(new Notification(getTranslation(LOGIN_FAILED),
                    getTranslation(LOGIN_FAILED_DESC),
                    Notification.Type.HUMANIZED_MESSAGE));
            usernameField.focus();
        }
    }

    private void showNotification(Notification notification) {
        // keep the notification visible a little while after moving the
        // mouse, or until clicked
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

    private static Logger logger = LoggerFactory.getLogger(LoginView.class);
}
