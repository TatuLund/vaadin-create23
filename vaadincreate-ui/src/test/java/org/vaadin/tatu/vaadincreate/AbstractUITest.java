package org.vaadin.tatu.vaadincreate;

import org.vaadin.tatu.vaadincreate.uiunittest.UIUnitTest;

import com.vaadin.ui.Button;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

public abstract class AbstractUITest extends UIUnitTest {

    protected void login() {
        var username = (TextField) $("login-username-field");
        username.setValue("Admin");
        var password = (PasswordField) $("login-password-field");
        password.setValue("admin");
        var loginButton = (Button) $("login-button");
        loginButton.click();
    }
}
