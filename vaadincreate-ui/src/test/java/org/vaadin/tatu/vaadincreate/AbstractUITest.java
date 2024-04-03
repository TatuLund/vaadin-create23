package org.vaadin.tatu.vaadincreate;

import org.vaadin.tatu.vaadincreate.i18n.DefaultI18NProvider;
import org.vaadin.tatu.vaadincreate.i18n.I18NProvider;
import org.vaadin.tatu.vaadincreate.login.LanguageSelect;

import com.vaadin.testbench.uiunittest.UIUnitTest;

import com.vaadin.ui.Button;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;

public abstract class AbstractUITest extends UIUnitTest {

    protected void login() {
        test($(TextField.class).id("login-username-field")).setValue("Admin");
        test($(PasswordField.class).id("login-password-field"))
                .setValue("admin");
        test($(LanguageSelect.class).first())
                .clickItem(DefaultI18NProvider.LOCALE_EN);
        test($(Button.class).id("login-button")).click();

    }

    protected void logout() {
        var menu = $(MenuBar.class).single();
        var menuItem = test(menu).item(2);
        test(menu).click(menuItem);
    }
}
