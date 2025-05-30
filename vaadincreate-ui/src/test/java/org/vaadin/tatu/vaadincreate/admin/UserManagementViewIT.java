package org.vaadin.tatu.vaadincreate.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.vaadin.tatu.vaadincreate.AbstractViewTest;

import com.vaadin.testbench.elements.ButtonElement;
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
    @Ignore("This is flake when running in GitHub Actions. Works ok on Windows.")
    @SuppressWarnings("java:S5961")
    public void createSaveAndDeleteUser() {
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

        var userSelect = $(ComboBoxElement.class).id("user-select");
        assertEquals("", userSelect.getValue());
        userSelect.click();
        userSelect.sendKeys("Testuser", Keys.ENTER);
        waitUntil(driver -> userSelect.getValue().equals("Testuser"));
        waitUntil(driver -> delete.isEnabled());

        assertFalse(save.isEnabled());
        assertTrue(delete.isEnabled());

        assertEquals("Testuser", user.getValue());
        assertEquals("testuser", password.getValue());
        assertEquals("testuser", passwordRepeat.getValue());
        assertEquals("USER", role.getValue());

        delete.click();

        var dialog = $(WindowElement.class).id("confirm-dialog");
        assertTrue(dialog.$(LabelElement.class).first().getText()
                .contains("Testuser"));
        dialog.$(ButtonElement.class).id("confirm-button").click();

        assertEquals("Käyttäjä \"Testuser\" poistettu.",
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
    public void visual() throws IOException {
        waitForElementPresent(By.id("new-button"));

        if (visualTests()) {
            assertTrue($(UIElement.class).first().compareScreen("user.png"));
        }
    }
}
