package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutAction.ModifierKey;
import com.vaadin.ui.Button;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class BooksViewBrowsingTest extends AbstractBooksViewTest {

    @Test
    public void alt_n_will_open_new_book_form_and_esc_will_close_it() {
        // WHEN: Pressing alt+N
        test($(Button.class).id("new-product")).shortcut(KeyCode.N,
                ModifierKey.ALT);

        // THEN: New book form is opened
        assertTrue(form.isShown());
        then_form_fields_are_reset_state();
        assertTrue(
                test($(form, TextField.class).id("product-name")).isFocused());

        // WHEN: Filling the form and pressing escape
        test($(form, TextField.class).id("product-name")).setValue("Test book");
        test($(form, Button.class).id("cancel-button"))
                .shortcut(KeyCode.ESCAPE);

        // THEN: Confirm dialog is shown
        var dialog = $(Window.class).id("confirm-dialog");
        var confirmButton = $(dialog, Button.class).id("confirm-button");

        // WHEN: Confirming the cancel by pressing enter
        test(confirmButton).shortcut(KeyCode.ENTER);

        // THEN: Form is closed
        assertFalse(form.isShown());
    }

    @Test
    public void browsing_products_keeps_focus_in_grid() {
        for (int i = 0; i < test(grid).size(); i += 10) {

            // WHEN: Clicking on a row
            test(grid).click(1, i);
            assertAssistiveNotification(String.format("%s opened",
                    test(grid).item(i).getProductName()));
            assertFalse($(Button.class).id("new-product").isEnabled());

            // THEN: Focus is still in the grid
            assertTrue(test(grid).isFocused());

            then_form_is_filled_with_values_from_grid_row(i);
            then_selected_categories_are_shown_first();
            then_availability_is_rendered_as_html(i);

            // WHEN: Clicking the row again
            test(grid).click(1, i);

            // THEN: Form is closed
            assertFalse(form.isShown());
            assertTrue($(Button.class).id("new-product").isEnabled());
            then_form_fields_are_reset_state();
        }

    }

    @Test
    @SuppressWarnings("java:S5961")
    public void when_browsing_products_with_pgDown_pgUp_keys_the_form_is_populated_accordingly_and_Esc_closes_the_form() {
        // WHEN: Clicking on the first row
        test(grid).click(1, 0);
        // THEN: Form is shown and populated with the first row data
        assertTrue(form.isShown());
        assertEquals(grid.getSelectedRow(), form.getProduct());
        then_form_is_filled_with_values_from_grid_row(0);

        // WHEN: Pressing page down
        test(form).shortcut(KeyCode.PAGE_DOWN);
        // THEN: Form is still shown and populated with the second row
        // data and that row is selected
        assertTrue(form.isShown());
        assertEquals(grid.getSelectedRow(), form.getProduct());
        then_form_is_filled_with_values_from_grid_row(1);

        // WHEN: Pressing page down
        test(form).shortcut(KeyCode.PAGE_DOWN);
        // THEN: Form is still shown and populated with the third row
        // data and that row is selected
        assertTrue(form.isShown());
        assertEquals(grid.getSelectedRow(), form.getProduct());
        then_form_is_filled_with_values_from_grid_row(2);

        // WHEN: Pressing page up
        test(form).shortcut(KeyCode.PAGE_UP);
        // THEN: Form is still shown and populated with the second row
        // data and that row is selected
        assertTrue(form.isShown());
        assertEquals(grid.getSelectedRow(), form.getProduct());
        then_form_is_filled_with_values_from_grid_row(1);

        // WHEN: Pressing page up
        test(form).shortcut(KeyCode.PAGE_UP);
        // THEN: Form is still shown and populated with the first row
        // data and that row is selected
        assertTrue(form.isShown());
        assertEquals(grid.getSelectedRow(), form.getProduct());
        then_form_is_filled_with_values_from_grid_row(0);

        // WHEN: Pressing page up
        test(form).shortcut(KeyCode.PAGE_UP);
        // THEN: Form is still shown and still populated with the first
        // row data as it is the first row and that row is still selected
        assertTrue(form.isShown());
        assertEquals(grid.getSelectedRow(), form.getProduct());
        then_form_is_filled_with_values_from_grid_row(0);

        // WHEN: Pressing escape
        test($(form, Button.class).id("cancel-button"))
                .shortcut(KeyCode.ESCAPE);
        // THEN: Form is closed
        assertFalse(form.isShown());
    }
}
