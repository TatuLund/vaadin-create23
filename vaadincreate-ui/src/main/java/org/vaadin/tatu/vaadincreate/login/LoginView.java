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
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.event.ConnectorEventListener;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.Page;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
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
public class LoginView extends Composite implements HasI18N {

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
    private CssLayout layout = new CssLayout();

    public LoginView(AccessControl accessControl, LoginListener loginListener) {
        this.accessControl = accessControl;
        addLoginListener(loginListener);
        setCompositionRoot(layout);
    }

    void buildUI() {
        layout.addStyleName(VaadinCreateTheme.LOGINVIEW);

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

        layout.addComponent(centeringLayout);
    }

    @Override
    public void attach() {
        super.attach();
        resizeReg = getUI().getPage().addBrowserWindowResizeListener(
                e -> showLoginInformation(e.getWidth()));
        buildUI();
        showLoginInformation(getUI().getPage().getBrowserWindowWidth());
        usernameField.focus();
    }

    private void updateTranslations() {
        if (getUI().getPage().getBrowserWindowWidth() < 700) {
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
        var h1 = createH1(getTranslation(I18n.Login.LOGIN_INFO));
        return String.format("%s %s", h1,
                getTranslation(I18n.Login.LOGIN_INFO_TEXT));
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
            layout.removeComponent(loginInformation);
            usernameField.setPlaceholder(getTranslation(I18n.USERNAME));
            passwordField.setPlaceholder(getTranslation(I18n.PASSWORD));
            usernameField.setCaption("");
            passwordField.setCaption("");
            lang.setCaption("");
        } else {
            layout.addComponent(loginInformation);
            usernameField.setPlaceholder("");
            passwordField.setPlaceholder("");
            usernameField.setCaption(getTranslation(I18n.USERNAME));
            passwordField.setCaption(getTranslation(I18n.PASSWORD));
            lang.setCaption(getTranslation(I18n.Login.LANGUAGE));
        }
    }

    private Component buildLoginForm() {
        var loginForm = new FormLayout();

        loginForm.addStyleName(VaadinCreateTheme.LOGINVIEW_FORM);
        loginForm.setSizeUndefined();
        loginForm.setMargin(false);

        usernameField = new TextField(getTranslation(I18n.USERNAME), "Admin");
        loginForm.addComponent(usernameField);
        usernameField.setWidth(18, Unit.EM);
        usernameField.setId("login-username-field");

        passwordField = new PasswordField(getTranslation(I18n.PASSWORD));
        loginForm.addComponent(passwordField);
        capsLockWarning = CapsLockWarning.warnFor(passwordField);
        capsLockWarning.setMessage(getTranslation(I18n.Login.CAPSLOCK));
        passwordField.setWidth(18, Unit.EM);
        passwordField.setId("login-password-field");
        CssLayout buttons = new CssLayout();
        buttons.setStyleName("buttons");
        loginForm.addComponent(buttons);

        login = new Button(getTranslation(I18n.Login.LOGIN_BUTTON));
        buttons.addComponent(login);
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

        forgotPassword = new Button(getTranslation(I18n.Login.FORGOT_PASSWORD));
        buttons.addComponent(forgotPassword);
        forgotPassword.addClickListener(event -> showHintNotification());
        forgotPassword.addStyleNames(ValoTheme.BUTTON_LINK,
                VaadinCreateTheme.LOGINVIEW_FORGOTBUTTON);

        lang = new LanguageSelect();
        lang.setWidth(18, Unit.EM);
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
        var infoLayout = new CssLayout();
        infoLayout.setStyleName(VaadinCreateTheme.LOGINVIEW_INFORMATION);
        loginInfoText = new Label(getLoginInfoText(), ContentMode.HTML);
        loginInfoText.setSizeFull();
        infoLayout.addComponent(loginInfoText);
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

    private static Logger logger = LoggerFactory.getLogger(LoginView.class);
}
