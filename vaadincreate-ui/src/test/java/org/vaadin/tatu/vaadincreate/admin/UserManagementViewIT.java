package org.vaadin.tatu.vaadincreate.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.vaadin.tatu.vaadincreate.AbstractViewTest;

import com.vaadin.testbench.elements.ButtonElement;
import com.vaadin.testbench.elements.ComboBoxElement;
import com.vaadin.testbench.elements.LabelElement;
import com.vaadin.testbench.elements.NotificationElement;
import com.vaadin.testbench.elements.PasswordFieldElement;
import com.vaadin.testbench.elements.TextFieldElement;
import com.vaadin.testbench.elements.WindowElement;

public class UserManagementViewIT extends AbstractViewTest {

    @Override
    public void setup() throws Exception {
        super.setup();
        login("Admin", "admin");
        open("#!" + AdminView.VIEW_NAME + "/" + UserManagementView.VIEW_NAME);
    }

    @Test
    public void createSaveAndDeleteUser() {
        waitForElementPresent(By.id("new-button"));

        var user = $(TextFieldElement.class).id("user-field");
        var password = $(PasswordFieldElement.class).id("password-field");
        var passwordRepeat = $(PasswordFieldElement.class)
                .id("password-repeat");
        var role = $(ComboBoxElement.class).id("role-field");

        assertFalse(user.isEnabled());
        assertFalse(password.isEnabled());
        assertFalse(passwordRepeat.isEnabled());
        assertFalse(role.isEnabled());

        $(ButtonElement.class).id("new-button").click();

        user.setValue("Testuser");
        password.setValue("testuser");
        assertTrue(password.getClassNames().contains("v-textfield-error"));
        passwordRepeat.setValue("testuser");
        assertFalse(password.getClassNames().contains("v-textfield-error"));
        role.selectByText("USER");
        assertFalse(role.getClassNames().contains("v-textfield-error"));

        $(ButtonElement.class).id("save-button").click();

        assertTrue($(NotificationElement.class).last().getText()
                .contains("Testuser"));

        assertEquals("", user.getValue());
        assertEquals("", password.getValue());
        assertEquals("", passwordRepeat.getValue());
        assertEquals("", role.getValue());

        var userSelect = $(ComboBoxElement.class).id("user-select");
        assertEquals("", userSelect.getValue());
        userSelect.selectByText("Testuser");

        assertEquals("Testuser", user.getValue());
        assertEquals("testuser", password.getValue());
        assertEquals("testuser", passwordRepeat.getValue());
        assertEquals("USER", role.getValue());

        $(ButtonElement.class).id("delete-button").click();

        var dialog = $(WindowElement.class).id("confirm-dialog");
        Assert.assertTrue(dialog.$(LabelElement.class).first().getText()
                .contains("Testuser"));
        dialog.$(ButtonElement.class).id("confirm-button").click();

        assertTrue($(NotificationElement.class).last().getText()
                .contains("Testuser"));

        assertEquals("", userSelect.getValue());

        assertEquals("", user.getValue());
        assertEquals("", password.getValue());
        assertEquals("", passwordRepeat.getValue());
        assertEquals("", role.getValue());
    }
}
