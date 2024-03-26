package org.vaadin.tatu.vaadincreate;

import org.vaadin.tatu.vaadincreate.uiunittest.UIUnitTest;

import com.vaadin.ui.Button;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

public abstract class AbstractUITest extends UIUnitTest {

    protected void login() {
        var username = $(TextField.class).id("login-username-field");
        test(username).setValue("Admin");
        var password = $(PasswordField.class).id("login-password-field");
        test(password).setValue("admin");
        var loginButton = $(Button.class).id("login-button");
        loginButton.click();
    }
}
