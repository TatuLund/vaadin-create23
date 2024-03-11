package org.vaadin.tatu.vaadincreate.login;

import java.lang.reflect.Method;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@SuppressWarnings("serial")
public class LoginView extends CssLayout implements HasI18N {

    private static final String LOGIN_INFO = "login-info";
    private static final String LOGIN_INFO_TEXT = "login-info-text";
    private static final String LOGIN_BUTTON = "login-button";
    private static final String LOGIN = "login";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String FORGOT_PASSWORD = "forgot-password";
    private static final String HINT = "hint";
    private static final String LANGUAGE = "language";

    TextField username;
    PasswordField password;
    Button login;
    private Button forgotPassword;
    private AccessControl accessControl;
    private Registration resizeReg;
    private CssLayout loginInformation;
    private VerticalLayout centeringLayout;
    private ComboBox<Locale> lang;
    private Label loginInfoText;

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
        centeringLayout = new VerticalLayout();
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
        resizeReg = getUI().getPage().addBrowserWindowResizeListener(e -> {
            showLoginInformation(e.getWidth());
        });
        buildUI();
        username.focus();
    }

    private void updateTranslations() {
        username.setCaption(getTranslation(USERNAME));
        password.setCaption(getTranslation(PASSWORD));
        login.setCaption(getTranslation(LOGIN_BUTTON));
        forgotPassword.setCaption(getTranslation(FORGOT_PASSWORD));
        loginInfoText.setValue("<h1>" + getTranslation(LOGIN_INFO) + "</h1>"
                + getTranslation(LOGIN_INFO_TEXT));
        lang.setCaption(getTranslation(LANGUAGE));
    }

    @Override
    public void detach() {
        super.detach();
        resizeReg.remove();
    }

    public void showLoginInformation(int width) {
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

        loginForm.addComponent(
                username = new TextField(getTranslation(USERNAME), "Admin"));
        username.setWidth(15, Unit.EM);
        loginForm.addComponent(
                password = new PasswordField(getTranslation(PASSWORD)));
        password.setWidth(15, Unit.EM);
        password.setDescription(getTranslation(HINT));
        CssLayout buttons = new CssLayout();
        buttons.setStyleName("buttons");
        loginForm.addComponent(buttons);

        buttons.addComponent(login = new Button(getTranslation(LOGIN_BUTTON)));
        login.setDisableOnClick(true);
        login.addClickListener(event -> {
            try {
                login();
            } finally {
                login.setEnabled(true);
            }
        });
        login.setClickShortcut(ShortcutAction.KeyCode.ENTER);
        login.addStyleName(ValoTheme.BUTTON_FRIENDLY);

        buttons.addComponent(
                forgotPassword = new Button(getTranslation(FORGOT_PASSWORD)));
        forgotPassword.addClickListener(event -> {
            showNotification(new Notification(getTranslation(HINT)));
        });
        forgotPassword.addStyleName(ValoTheme.BUTTON_LINK);

        lang = new LanguageSelect();
        loginForm.addComponent(lang);
        lang.addValueChangeListener(e -> {
            var ui = getUI();
            // Set the locale to session attribute, request handler will persist
            // it to the cookie from there.
            ui.getSession().setAttribute("locale", e.getValue().getLanguage());
            ui.getSession().setLocale(e.getValue());
            updateTranslations();
            logger.info("Changing locale to {}", e.getValue().getLanguage());
        });
        lang.setValue(I18NProvider.fetchLocaleFromCookie());

        return loginForm;
    }

    private CssLayout buildLoginInformation() {
        var loginInformation = new CssLayout();
        loginInformation.setStyleName(VaadinCreateTheme.LOGINVIEW_INFORMATION);
        loginInfoText = new Label("<h1>" + getTranslation(LOGIN_INFO) + "</h1>"
                + getTranslation(LOGIN_INFO_TEXT), ContentMode.HTML);
        loginInfoText.setSizeFull();
        loginInformation.addComponent(loginInfoText);
        return loginInformation;
    }

    private void login() {
        if (accessControl.signIn(username.getValue(), password.getValue())) {
            fireEvent(new LoginEvent(this));
        } else {
            showNotification(new Notification("Login failed",
                    "Please check your username and password and try again.",
                    Notification.Type.HUMANIZED_MESSAGE));
            username.focus();
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

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
