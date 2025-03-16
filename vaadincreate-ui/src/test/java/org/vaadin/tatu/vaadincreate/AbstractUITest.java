package org.vaadin.tatu.vaadincreate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.vaadin.tatu.vaadincreate.crud.BookGrid;
import org.vaadin.tatu.vaadincreate.crud.FakeGrid;
import org.vaadin.tatu.vaadincreate.i18n.DefaultI18NProvider;
import org.vaadin.tatu.vaadincreate.login.LanguageSelect;

import com.vaadin.server.VaadinSession;
import com.vaadin.testbench.uiunittest.UIUnitTest;

import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;

public abstract class AbstractUITest extends UIUnitTest {

    /**
     * Login as "Admin"
     */
    protected void login() {
        login("Admin", "admin");
    }

    /**
     * Login as user
     *
     * @param username
     *            The username
     * @param password
     *            The password
     */
    protected void login(String username, String password) {
        var sessionId = VaadinSession.getCurrent().getSession().getId();
        test($(TextField.class).id("login-username-field")).setValue(username);
        test($(PasswordField.class).id("login-password-field"))
                .setValue(password);
        test($(LanguageSelect.class).first())
                .clickItem(DefaultI18NProvider.LOCALE_EN);
        test($(Button.class).id("login-button")).click();
        // Session fixation is not working with nginx proxy
        assertEquals(sessionId,
                VaadinSession.getCurrent().getSession().getId());
    }

    /**
     * Trigger logout by clicking logout menu.
     */
    protected void logout() {
        var menu = $(MenuBar.class).single();
        var menuItem = test(menu).item(2);
        test(menu).click(menuItem);
    }

    /**
     * Wait until Grid is present in the layout
     *
     * @param layout
     *            The layout
     * @param grid
     *            BookGrid grid
     */
    protected void waitForGrid(HasComponents layout, BookGrid grid) {
        assertFalse(grid.isVisible());

        var fake = $(layout, FakeGrid.class).first();
        waitWhile(fake, f -> f.isVisible(), 10);
        assertTrue(grid.isVisible());
    }

    /**
     * Wait until charts have been loaded in the dashboard.
     *
     * @param dashboard
     *            The dashboard
     */
    protected void waitForCharts(CssLayout dashboard) {
        waitWhile(dashboard, d -> !d.getStyleName().contains("loaded"), 15);
    }

}
