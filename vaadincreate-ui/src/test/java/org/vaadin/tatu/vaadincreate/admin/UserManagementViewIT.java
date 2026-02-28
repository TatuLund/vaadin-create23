package org.vaadin.tatu.vaadincreate.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.openqa.selenium.By;
import org.vaadin.tatu.vaadincreate.AbstractViewTest;

import com.deque.html.axecore.selenium.AxeBuilder;
import com.vaadin.testbench.elements.ButtonElement;
import com.vaadin.testbench.elements.CheckBoxElement;
import com.vaadin.testbench.elements.ComboBoxElement;
import com.vaadin.testbench.elements.LabelElement;
import com.vaadin.testbench.elements.NotificationElement;
import com.vaadin.testbench.elements.PasswordFieldElement;
import com.vaadin.testbench.elements.TextFieldElement;
import com.vaadin.testbench.elements.UIElement;
import com.vaadin.testbench.elements.WindowElement;

public class UserManagementViewIT extends AbstractViewTest {

    @Override
    public void setup() {
        super.setup();
        open("#!" + AdminView.VIEW_NAME + "/" + UserManagementView.VIEW_NAME);
        login("Admin", "admin");
    }

    @After
    public void cleanup() {
        logout();
    }

    @Test
    @SuppressWarnings("java:S5961")
    public void createAndSaveTestuser() {
        waitForElementPresent(By.id("new-button"));

        var user = $(TextFieldElement.class).id("user-field");
        var password = $(PasswordFieldElement.class).id("password-field");
        var passwordRepeat = $(PasswordFieldElement.class)
                .id("password-repeat");
        var role = $(ComboBoxElement.class).id("role-field");

        var delete = $(ButtonElement.class).id("delete-button");
        var save = $(ButtonElement.class).id("save-button");

        assertFalse(delete.isEnabled());
        assertFalse(save.isEnabled());

        assertFalse(user.isEnabled());
        assertFalse(password.isEnabled());
        assertFalse(passwordRepeat.isEnabled());
        assertFalse(role.isEnabled());

        $(ButtonElement.class).id("new-button").click();

        user.setValue("Testuser");
        password.setValue("testuser");
        assertTrue(password.getClassNames().contains("v-textfield-error"));
        assertFalse(save.isEnabled());
        passwordRepeat.setValue("testuser");
        assertFalse(password.getClassNames().contains("v-textfield-error"));
        role.selectByText("USER");
        assertFalse(role.getClassNames().contains("v-textfield-error"));

        assertTrue(save.isEnabled());
        save.click();

        var notification = $(NotificationElement.class).last();
        assertEquals("Käyttäjä \"Testuser\" tallenettu.",
                notification.getText());

        assertFalse(delete.isEnabled());
        assertFalse(save.isEnabled());

        assertEquals("", user.getValue());
        assertEquals("", password.getValue());
        assertEquals("", passwordRepeat.getValue());
        assertEquals("", role.getValue());
    }

    @Test
    public void deleteUser4() {
        waitForElementPresent(By.id("new-button"));

        var user = $(TextFieldElement.class).id("user-field");
        var role = $(ComboBoxElement.class).id("role-field");
        var delete = $(ButtonElement.class).id("delete-button");
        var save = $(ButtonElement.class).id("save-button");
        var password = $(PasswordFieldElement.class).id("password-field");
        var passwordRepeat = $(PasswordFieldElement.class)
                .id("password-repeat");

        var userSelect = $(ComboBoxElement.class).id("user-select");
        userSelect.selectByText("User4");

        assertFalse(save.isEnabled());
        assertTrue(delete.isEnabled());

        delete.click();

        var dialog = $(WindowElement.class).id("confirm-dialog");
        assertTrue(dialog.$(LabelElement.class).first().getText()
                .contains("User4"));
        dialog.$(ButtonElement.class).id("confirm-button").click();

        assertEquals("Käyttäjä \"User4\" poistettu.",
                $(NotificationElement.class).last().getText());

        assertEquals("", userSelect.getValue());

        assertFalse(delete.isEnabled());
        assertFalse(save.isEnabled());

        assertEquals("", user.getValue());
        assertEquals("", password.getValue());
        assertEquals("", passwordRepeat.getValue());
        assertEquals("", role.getValue());
    }

    @Test
    public void findUser2() {
        waitForElementPresent(By.id("new-button"));

        var user = $(TextFieldElement.class).id("user-field");
        var role = $(ComboBoxElement.class).id("role-field");
        var delete = $(ButtonElement.class).id("delete-button");
        var save = $(ButtonElement.class).id("save-button");
        var password = $(PasswordFieldElement.class).id("password-field");
        var passwordRepeat = $(PasswordFieldElement.class)
                .id("password-repeat");

        assertFalse(delete.isEnabled());
        assertFalse(save.isEnabled());

        assertEquals("", user.getValue());
        assertEquals("", password.getValue());
        assertEquals("", passwordRepeat.getValue());
        assertEquals("", role.getValue());

        var userSelect = $(ComboBoxElement.class).id("user-select");
        assertEquals("", userSelect.getValue());
        userSelect.selectByText("User2");

        assertFalse(save.isEnabled());
        assertTrue(delete.isEnabled());

        assertEquals("User2", user.getValue());
        assertEquals("USER", role.getValue());
    }

    @Test
    public void visual() throws IOException {
        if (visualTests()) {
            waitForElementPresent(By.id("new-button"));
            $(ComboBoxElement.class).id("user-select").selectByText("User2");
            $(CheckBoxElement.class).id("active-field").clear();
            wait(Duration.ofMillis(200));
            assertTrue($(UIElement.class).first().compareScreen("user.png"));
        }
    }

    @Test
    public void accessibility() {
        waitForElementPresent(By.id("new-button"));

        var userSelect = $(ComboBoxElement.class).id("user-select");
        assertEquals("", userSelect.getValue());
        userSelect.selectByText("User2");

        var axeBuilder = new AxeBuilder();
        axeBuilder.exclude(".v-tooltip");
        axeBuilder
                .disableRules(List.of("color-contrast", "autocomplete-valid"));

        var axeResults = axeBuilder.analyze(driver);
        logViolations(axeResults);
        assertTrue(axeResults.violationFree());
    }
}
