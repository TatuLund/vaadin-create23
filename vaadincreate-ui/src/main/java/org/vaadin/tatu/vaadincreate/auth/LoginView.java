package org.vaadin.tatu.vaadincreate.auth;

import java.io.Serializable;

import com.vaadin.event.ShortcutAction;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * UI content when the user is not logged in yet.
 */
@SuppressWarnings("serial")
public class LoginView extends CssLayout {

    private static final String LOGINVIEW_INFORMATION = "loginview-information";
    private static final String LOGINVIEW_FORM = "loginview-form";
    private static final String LOGINVIEW_CENTER = "loginview-center";
    private static final String LOGINVIEW = "loginview";
    private TextField username;
    private PasswordField password;
    private Button login;
    private Button forgotPassword;
    private LoginListener loginListener;
    private AccessControl accessControl;

    public LoginView(AccessControl accessControl, LoginListener loginListener) {
        this.loginListener = loginListener;
        this.accessControl = accessControl;
        buildUI();
        username.focus();
    }

    private void buildUI() {
        addStyleName(LOGINVIEW);

        // login form, centered in the available part of the screen
        var loginForm = buildLoginForm();

        // layout to center login form when there is sufficient screen space
        // - see the theme for how this is made responsive for various screen
        // sizes
        var centeringLayout = new VerticalLayout();
        centeringLayout.setMargin(false);
        centeringLayout.setSpacing(false);
        centeringLayout.setStyleName(LOGINVIEW_CENTER);
        centeringLayout.addComponent(loginForm);
        centeringLayout.setComponentAlignment(loginForm,
                Alignment.MIDDLE_CENTER);

        // information text about logging in
        var loginInformation = buildLoginInformation();

        addComponent(centeringLayout);
        addComponent(loginInformation);
    }

    private Component buildLoginForm() {
        var loginForm = new FormLayout();

        loginForm.addStyleName(LOGINVIEW_FORM);
        loginForm.setSizeUndefined();
        loginForm.setMargin(false);

        loginForm.addComponent(username = new TextField("Username", "Admin"));
        username.setWidth(15, Unit.EM);
        loginForm.addComponent(password = new PasswordField("Password"));
        password.setWidth(15, Unit.EM);
        password.setDescription("Write anything");
        CssLayout buttons = new CssLayout();
        buttons.setStyleName("buttons");
        loginForm.addComponent(buttons);

        buttons.addComponent(login = new Button("Login"));
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

        buttons.addComponent(forgotPassword = new Button("Forgot password?"));
        forgotPassword.addClickListener(event -> {
            showNotification(new Notification(
                    "Hint: Try User0 / user0 or Admin / admin"));
        });
        forgotPassword.addStyleName(ValoTheme.BUTTON_LINK);
        return loginForm;
    }

    private CssLayout buildLoginInformation() {
        var loginInformation = new CssLayout();
        loginInformation.setStyleName(LOGINVIEW_INFORMATION);
        var loginInfoText = new Label("<h1>Login Information</h1>"
                + "Log in as &quot;Admin&quot; to have full access. Log in with any other &quot;UserX&quot; to have read-only access. For all users, any &quot;userX&quot; is fine",
                ContentMode.HTML);
        loginInfoText.setSizeFull();
        loginInformation.addComponent(loginInfoText);
        return loginInformation;
    }

    private void login() {
        if (accessControl.signIn(username.getValue(), password.getValue())) {
            loginListener.loginSuccessful();
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

    public interface LoginListener extends Serializable {
        void loginSuccessful();
    }
}
