package org.vaadin.tatu.vaadincreate.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        navigate(AdminView.VIEW_NAME + "/" + UserManagementView.VIEW_NAME,
                AdminView.class);
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
    }

    @Test
    public void verify_that_initially_the_form_is_empty_and_buttons_are_not_enabled() {
        then_form_is_empty_and_buttons_are_disabled();
    }

    private void then_form_is_empty_and_buttons_are_disabled() {
        assertFalse($(Button.class).id("delete-button").isEnabled());
        assertFalse($(Button.class).id("save-button").isEnabled());
        assertFalse($(Button.class).id("cancel-button").isEnabled());
        assertTrue($(Button.class).id("new-button").isEnabled());
        assertFalse($(FormLayout.class).single().isEnabled());
        assertEquals("", $(TextField.class).id("user-field").getValue());
        assertEquals("",
                $(PasswordField.class).id("password-field").getValue());
        assertEquals("",
                $(PasswordField.class).id("password-repeat").getValue());
        assertNull($(ComboBox.class).id("role-field").getValue());
    }

    @SuppressWarnings({ "unchecked", "java:S5961" })
    @Test
    public void add_user_while_asserting_input_validation_clicking_save_will_show_message_and_clear_the_form_concurrent_edit_will_result_an_error() {
        // WHEN: Clicking new user button
        test($(Button.class).id("new-button")).click();

        // THEN: Form is enabled and focused on the first field
        var save = $(Button.class).id("save-button");
        assertFalse($(Button.class).id("delete-button").isEnabled());
        assertFalse(save.isEnabled());
        assertTrue($(FormLayout.class).single().isEnabled());
        assertTrue(test($(TextField.class).id("user-field")).isFocused());

        // WHEN: Filling user name
        test($(TextField.class).id("user-field")).setValue("Tester");

        // THEN: Save button is disabled and cancel button is enabled
        assertFalse(save.isEnabled());
        assertTrue($(Button.class).id("cancel-button").isEnabled());

        // WHEN: Filling password
        test($(PasswordField.class).id("password-field")).setValue("tester");

        // THEN: Password field is invalid, validation message is shown and save
        // button is disabled
        assertTrue(
                test($(PasswordField.class).id("password-field")).isInvalid());
        assertEquals("Passwords do not match",
                test($(PasswordField.class).id("password-field"))
                        .errorMessage());
        assertFalse(save.isEnabled());

        // WHEN: Filling password repeat
        test($(PasswordField.class).id("password-repeat")).setValue("tester");

        // THEN: Password field is valid, save button is disabled and role is
        // invalid and validation message is shown
        assertFalse(
                test($(PasswordField.class).id("password-field")).isInvalid());
        assertFalse(save.isEnabled());
        assertTrue(test($(ComboBox.class).id("role-field")).isInvalid());
        assertEquals("The role is mandatory",
                test($(ComboBox.class).id("role-field")).errorMessage());

        // WHEN: Filling role
        test($(ComboBox.class).id("role-field")).clickItem(Role.USER);

        // THEN: Role is valid, save button is enabled and delete button is
        // disabled
        assertFalse(test($(ComboBox.class).id("role-field")).isInvalid());
        assertFalse($(Button.class).id("delete-button").isEnabled());
        assertTrue(save.isEnabled());

        // WHEN: Clicking save
        test(save).click();

        // THEN: Notification is shown and form is cleared
        assertEquals("User \"Tester\" saved.",
                $(Notification.class).last().getCaption());

        then_form_is_empty_and_buttons_are_disabled();

        // WHEN: Selecting the saved user
        test($(ComboBox.class).id("user-select")).setInput("Tester");

        // THEN: Form is enabled and fields are filled
        assertTrue($(FormLayout.class).single().isEnabled());
        assertEquals("Tester", $(TextField.class).id("user-field").getValue());

        // WHEN: Simulating other user editing the saved user
        var user = ui.getUserService().findByName("Tester").get();
        ui.getUserService().updateUser(user);

        // WHEN: Editing the newly saved user and clicking save button
        test($(ComboBox.class).id("user-select")).setInput("Tester");
        test($(TextField.class).id("user-field")).setValue("Mocker");
        test(save).click();

        // THEN: Save conflict message is shown and form is cleared
        assertEquals("Save conflict, try again.",
                $(Notification.class).last().getCaption());

        then_form_is_empty_and_buttons_are_disabled();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void clicking_delete_will_present_confirm_dialog_and_confirming_will_show_delete_message_and_form_will_be_cleared() {
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
        then_form_is_empty_and_buttons_are_disabled();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void clicking_cancel_will_clear_the_form() {
        // WHEN: Editing a user and clicking cancel
        test($(ComboBox.class).id("user-select")).setInput("User1");
        assertTrue($(FormLayout.class).single().isEnabled());

        test($(TextField.class).id("user-field")).setValue("Modified");
        assertTrue($(Button.class).id("cancel-button").isEnabled());

        test($(Button.class).id("cancel-button")).click();

        // THEN: The form is cleared
        then_form_is_empty_and_buttons_are_disabled();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void saving_with_duplicate_name_will_show_error() {
        // WHEN: Adding a new user with a duplicate name
        test($(Button.class).id("new-button")).click();

        test($(TextField.class).id("user-field")).setValue("Super");
        test($(PasswordField.class).id("password-field")).setValue("tester");
        test($(PasswordField.class).id("password-repeat")).setValue("tester");
        test($(ComboBox.class).id("role-field")).clickItem(Role.USER);

        test($(Button.class).id("save-button")).click();

        // THEN: An error message is shown
        assertEquals("Username \"Super\" is a duplicate.",
                $(Notification.class).last().getCaption());
        $(Notification.class).last().close();

        then_form_is_empty_and_buttons_are_disabled();
    }
}
