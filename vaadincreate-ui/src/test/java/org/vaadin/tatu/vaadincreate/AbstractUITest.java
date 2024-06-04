package org.vaadin.tatu.vaadincreate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.vaadin.tatu.vaadincreate.crud.BookGrid;
import org.vaadin.tatu.vaadincreate.crud.FakeGrid;
import org.vaadin.tatu.vaadincreate.i18n.DefaultI18NProvider;
import org.vaadin.tatu.vaadincreate.login.LanguageSelect;

import com.vaadin.server.VaadinSession;
import com.vaadin.testbench.uiunittest.UIUnitTest;

import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public abstract class AbstractUITest extends UIUnitTest {

    protected void login() {
        var sessionId = VaadinSession.getCurrent().getSession().getId();
        test($(TextField.class).id("login-username-field")).setValue("Admin");
        test($(PasswordField.class).id("login-password-field"))
                .setValue("admin");
        test($(LanguageSelect.class).first())
                .clickItem(DefaultI18NProvider.LOCALE_EN);
        test($(Button.class).id("login-button")).click();
        assertNotEquals(sessionId,
                VaadinSession.getCurrent().getSession().getId());
    }

    protected void logout() {
        var menu = $(MenuBar.class).single();
        var menuItem = test(menu).item(2);
        test(menu).click(menuItem);
    }

    protected void waitForGrid(VerticalLayout layout, BookGrid grid) {
        assertFalse(grid.isVisible());

        var fake = $(layout, FakeGrid.class).first();
        waitWhile(fake, f -> f.isVisible(), 10);
        assertTrue(grid.isVisible());
    }

    protected void waitForCharts(VerticalLayout layout, CssLayout dashboard) {
        assertFalse(dashboard.getStyleName().contains("loaded"));
        waitWhile(dashboard, d -> !d.getStyleName().contains("loaded"), 15);
    }

}
