package org.vaadin.tatu.vaadincreate.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

import com.vaadin.server.ServiceException;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class UserManagementViewTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private AdminView view;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        view = navigate(
                AdminView.VIEW_NAME + "/" + UserManagementView.VIEW_NAME,
                AdminView.class);
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
    }

    @Test
    public void initialState() {
        assertFalse($(Button.class).id("delete-button").isEnabled());
        assertFalse($(Button.class).id("save-button").isEnabled());
        assertTrue($(Button.class).id("new-button").isEnabled());
        assertFalse($(FormLayout.class).single().isEnabled());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addUser() {
        $(Button.class).id("new-button").click();

        var save = $(Button.class).id("save-button");

        assertFalse($(Button.class).id("delete-button").isEnabled());
        assertFalse(save.isEnabled());

        assertTrue($(FormLayout.class).single().isEnabled());
        assertTrue(test($(TextField.class).id("user-field")).isFocused());
        test($(TextField.class).id("user-field")).setValue("Tester");
        assertFalse(save.isEnabled());

        test($(PasswordField.class).id("password-field")).setValue("tester");
        assertTrue(
                test($(PasswordField.class).id("password-field")).isInvalid());
        assertEquals("Passwords do not match",
                test($(PasswordField.class).id("password-field"))
                        .errorMessage());
        assertFalse(save.isEnabled());

        test($(PasswordField.class).id("password-repeat")).setValue("tester");
        assertFalse(
                test($(PasswordField.class).id("password-field")).isInvalid());
        assertFalse(save.isEnabled());

        assertTrue(test($(ComboBox.class).id("role-field")).isInvalid());
        assertEquals("The role is mandatory",
                test($(ComboBox.class).id("role-field")).errorMessage());
        test($(ComboBox.class).id("role-field")).clickItem(Role.USER);
        assertFalse(test($(ComboBox.class).id("role-field")).isInvalid());

        assertFalse($(Button.class).id("delete-button").isEnabled());
        assertTrue(save.isEnabled());

        test(save).click();

        assertEquals("User \"Tester\" saved.",
                $(Notification.class).last().getCaption());
        assertFalse($(FormLayout.class).single().isEnabled());

        // Check that new user is there
        test($(ComboBox.class).id("user-select")).setInput("Tester");
        assertTrue($(FormLayout.class).single().isEnabled());
        assertEquals("Tester", $(TextField.class).id("user-field").getValue());

        // Simulate other user editing the user
        var user = ui.getUserService().findByName("Tester").get();
        ui.getUserService().updateUser(user);

        // Edit the user again
        test($(ComboBox.class).id("user-select")).setInput("Tester");
        test($(TextField.class).id("user-field")).setValue("Mocker");
        test(save).click();

        // Assert that optimistic locking is thrown and cought
        assertEquals("Save conflict, try again.",
                $(Notification.class).last().getCaption());
        assertFalse($(FormLayout.class).single().isEnabled());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void deleteUser() {
        test($(ComboBox.class).id("user-select")).setInput("User0");
        assertTrue($(FormLayout.class).single().isEnabled());

        test($(Button.class).id("delete-button")).click();

        var dialog = $(Window.class).id("confirm-dialog");
        assertEquals("\"User0\" will be deleted.",
                $(dialog, Label.class).single().getValue());
        test($(dialog, Button.class).id("confirm-button")).click();

        assertEquals("User \"User0\" removed.",
                $(Notification.class).last().getCaption());

        assertFalse($(FormLayout.class).single().isEnabled());

        test($(ComboBox.class).id("user-select")).setInput("User0");
        assertFalse($(FormLayout.class).single().isEnabled());
    }
}
