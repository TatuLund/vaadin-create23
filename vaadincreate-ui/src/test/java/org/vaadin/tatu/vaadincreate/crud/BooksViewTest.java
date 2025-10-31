package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;

import org.jsoup.Jsoup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AboutView;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.AppLayout.MenuButton;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.crud.form.AvailabilitySelector;
import org.vaadin.tatu.vaadincreate.crud.form.BookForm;
import org.vaadin.tatu.vaadincreate.crud.form.NumberField;
import org.vaadin.tatu.vaadincreate.locking.LockedObjects;

import com.vaadin.data.ValueContext;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutAction.ModifierKey;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class BooksViewTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private BooksView view;
    private BookGrid grid;
    private BookForm form;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        view = navigate(BooksView.VIEW_NAME, BooksView.class);
        assertAssistiveNotification("Inventory opened");

        var layout = $(view, VerticalLayout.class).first();
        grid = $(layout, BookGrid.class).single();
        waitForGrid(layout, grid);
        form = $(view, BookForm.class).single();
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
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
        // THEN: Form is still shown and populated with the second row data and
        // that row is selected
        assertTrue(form.isShown());
        assertEquals(grid.getSelectedRow(), form.getProduct());
        then_form_is_filled_with_values_from_grid_row(1);

        // WHEN: Pressing page down
        test(form).shortcut(KeyCode.PAGE_DOWN);
        // THEN: Form is still shown and populated with the third row data and
        // that row is selected
        assertTrue(form.isShown());
        assertEquals(grid.getSelectedRow(), form.getProduct());
        then_form_is_filled_with_values_from_grid_row(2);

        // WHEN: Pressing page up
        test(form).shortcut(KeyCode.PAGE_UP);
        // THEN: Form is still shown and populated with the second row data and
        // that row is selected
        assertTrue(form.isShown());
        assertEquals(grid.getSelectedRow(), form.getProduct());
        then_form_is_filled_with_values_from_grid_row(1);

        // WHEN: Pressing page up
        test(form).shortcut(KeyCode.PAGE_UP);
        // THEN: Form is still shown and populated with the first row data and
        // that row is selected
        assertTrue(form.isShown());
        assertEquals(grid.getSelectedRow(), form.getProduct());
        then_form_is_filled_with_values_from_grid_row(0);

        // WHEN: Pressing page up
        test(form).shortcut(KeyCode.PAGE_UP);
        // THEN: Form is still shown and still populated with the first row data
        // as it is the first row and that row is still selected
        assertTrue(form.isShown());
        assertEquals(grid.getSelectedRow(), form.getProduct());
        then_form_is_filled_with_values_from_grid_row(0);

        // WHEN: Pressing escape
        test($(form, Button.class).id("cancel-button"))
                .shortcut(KeyCode.ESCAPE);
        // THEN: Form is closed
        assertFalse(form.isShown());
    }

    @SuppressWarnings("java:S100")
    private void then_availability_is_rendered_as_html(int i) {
        var book = test(grid).item(i);
        String color = "";
        switch (book.getAvailability()) {
        case AVAILABLE -> color = VaadinCreateTheme.COLOR_AVAILABLE;
        case DISCONTINUED -> color = VaadinCreateTheme.COLOR_DISCONTINUED;
        case COMING -> color = VaadinCreateTheme.COLOR_COMING;
        }
        var doc = Jsoup.parse((String) test(grid).cell(3, i));
        assertEquals("v-icon",
                doc.getElementsByTag("span").get(0).attr("class"));
        assertEquals(String.format("font-family: Vaadin-Icons;color:%s", color),
                doc.getElementsByTag("span").get(0).attr("style"));
        assertEquals(
                VaadinCreateTheme.BOOKVIEW_AVAILABILITYLABEL + "-aria-label",
                doc.getElementsByTag("span").get(1).attr("class"));
        assertEquals(availability(book),
                doc.getElementsByTag("span").get(1).attr("aria-label"));
        assertEquals(VaadinCreateTheme.BOOKVIEW_AVAILABILITYLABEL,
                doc.getElementsByTag("span").get(2).attr("class"));
        assertEquals(book.getAvailability().toString(),
                doc.getElementsByTag("span").get(2).text());
    }

    private static String availability(Product book) {
        if (book.getAvailability() == Availability.AVAILABLE) {
            return String.format("%s %d", book.getAvailability(),
                    book.getStockCount());
        }
        return book.getAvailability().toString();
    }

    private void then_form_is_filled_with_values_from_grid_row(int i) {
        var book = test(grid).item(i);
        assertEquals(book.getProductName(),
                $(form, TextField.class).id("product-name").getValue());
        var price = $(form, TextField.class).id("price");
        var converter = new EuroConverter("");
        assertEquals(
                converter.convertToPresentation(book.getPrice(),
                        new ValueContext(null, price, ui.getLocale())),
                price.getValue());
        assertEquals(book.getStockCount(),
                $(form, NumberField.class).id("stock-count").getValue());

        assertEquals(book.getAvailability(), $(form, AvailabilitySelector.class)
                .id("availability").getValue());
        assertEquals(book.getCategory(),
                $(form, CheckBoxGroup.class).id("category").getValue());
    }

    private void then_form_fields_are_reset_state() {
        assertEquals("",
                $(form, TextField.class).id("product-name").getValue());
        assertEquals(Integer.valueOf(0),
                $(form, NumberField.class).id("stock-count").getValue());
        assertEquals("0.00 €", $(form, TextField.class).id("price").getValue());
        assertEquals(Availability.COMING, $(form, AvailabilitySelector.class)
                .id("availability").getValue());
        assertEquals(Collections.emptySet(),
                $(form, CheckBoxGroup.class).id("category").getValue());
    }

    private void then_selected_categories_are_shown_first() {
        // Verify that the selected categories are the first in the list
        var items = $(form, CheckBoxGroup.class).id("category")
                .getDataCommunicator().fetchItemsWithRange(0,
                        $(form, CheckBoxGroup.class).id("category")
                                .getDataCommunicator().getDataProviderSize());
        var size = $(form, CheckBoxGroup.class).id("category").getValue()
                .size();
        for (int j = 0; j < size; j++) {
            assertTrue($(form, CheckBoxGroup.class).id("category").getValue()
                    .contains(items.get(j)));
        }
    }

    @Test
    public void pressing_ctrl_F_will_focus_filter_field() {
        // GIVEN: Focus is in the grid
        var filter = $(FilterField.class).id("filter-field");
        assertFalse(test(filter).isFocused());
        // WHEN: Pressing ctrl+F
        test(filter).shortcut(KeyCode.F, ModifierKey.CTRL);
        // THEN: Focus is in the filter field
        assertTrue(test(filter).isFocused());
    }

    @Test
    @SuppressWarnings({ "unchecked", "java:S5961" })
    public void input_wrong_stock_count_vs_availability_shows_validation_error_and_cancel_shows_confirm_dialog_and_after_discarding_edits_cancel_closes_form() {
        // WHEN: Opening empty product form
        test($(view, Button.class).id("new-product")).click();

        // THEN: Discard and save buttons on the form are disabled
        assertFalse($(form, Button.class).id("discard-button").isEnabled());
        assertFalse($(form, Button.class).id("save-button").isEnabled());

        // WHEN: Filling the form with wrong stock count and availability and
        // clicking save button
        test($(form, TextField.class).id("product-name")).setValue("Te");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.COMING);
        test($(form, TextField.class).id("price")).setValue("10.0 €");
        test($(form, NumberField.class).id("stock-count")).setValue(10);
        var cat = ui.getProductService().getAllCategories().stream().findFirst()
                .get();
        test($(form, CheckBoxGroup.class).id("category")).clickItem(cat);

        test($(form, Button.class).id("save-button")).click();

        // THEN: Form is still shown and save button is disabled and validation
        // error is shown in stock count and availability fields
        assertFalse($(form, Button.class).id("save-button").isEnabled());
        assertTrue(test($(form, AvailabilitySelector.class).id("availability"))
                .isInvalid());
        var errorMessage = "Mismatch between availability and stock count";
        assertEquals(errorMessage,
                test($(form, AvailabilitySelector.class).id("availability"))
                        .errorMessage());
        assertTrue(
                test($(form, NumberField.class).id("stock-count")).isInvalid());
        assertEquals(errorMessage,
                test($(form, NumberField.class).id("stock-count"))
                        .errorMessage());

        // WHEN: Inputting correct stock count
        test($(form, NumberField.class).id("stock-count")).setValue(0);

        // THEN: Validation error is removed from stock count and availability
        // and save button is enabled
        assertFalse(test($(form, AvailabilitySelector.class).id("availability"))
                .isInvalid());
        assertFalse(
                test($(form, NumberField.class).id("stock-count")).isInvalid());
        assertTrue($(form, Button.class).id("save-button").isEnabled());

        // WHEN: Cancelling the form by clicking cancel button
        test($(form, Button.class).caption("Cancel").single()).click();

        // THEN: Confirm dialog is shown
        var dialog = $(Window.class).id("confirm-dialog");

        // WHEN: Cancelling the cancel
        test($(dialog, Button.class).id("cancel-button")).click();

        // THEN: Form is still shown
        assertTrue(form.isShown());

        // WHEN: Clicking discard button
        test($(form, Button.class).caption("Discard").single()).click();

        // THEN: The fields have empty and default values
        assertEquals("",
                $(form, TextField.class).id("product-name").getValue());
        assertEquals("0.00 €", $(form, TextField.class).id("price").getValue());
        assertTrue($(form, CheckBoxGroup.class).id("category").getValue()
                .isEmpty());

        // WHEN: Cancelling the form by clicking cancel button
        test($(form, Button.class).caption("Cancel").single()).click();

        // THEN: The form is closed and grid is focused
        assertFalse(form.isShown());
        assertTrue(test(grid).isFocused());
    }

    @Test
    @SuppressWarnings({ "unchecked", "java:S5961" })
    public void creating_new_product_and_saving_it() {
        // WHEN: Opening empty product form
        test($(view, Button.class).id("new-product")).click();

        // THEN: Form is shown and product name field is focused
        assertFalse($(view, Button.class).id("new-product").isEnabled());
        assertTrue(
                test($(form, TextField.class).id("product-name")).isFocused());

        // WHEN: Filling the form
        test($(form, TextField.class).id("product-name"))
                .setValue("Filter book");
        test($(form, TextField.class).id("price")).setValue("10.0 €");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);
        test($(form, NumberField.class).id("stock-count")).setValue(10);

        // WHEN: Selecting a category
        var cat = ui.getProductService().getAllCategories().stream().findFirst()
                .get();
        test($(form, CheckBoxGroup.class).id("category")).clickItem(cat);
        then_selected_categories_are_shown_first();

        // WHEN: Saving the form
        test($(form, Button.class).id("save-button")).click();

        // THEN: Notification is shown with the book name, form is closed and
        // grid is focused
        assertFalse(form.isShown());
        assertTrue(test(grid).isFocused());
        assertNotification("\"Filter book\" updated");
        assertTrue($(view, Button.class).id("new-product").isEnabled());

        // THEN: The book is added to the backend
        assertTrue(ui.getProductService().getAllProducts().stream()
                .anyMatch(b -> b.getProductName().equals("Filter book")));

        // THEN: The new book is added to the end
        int row = test(grid).size() - 1;
        assertEquals("Filter book", test(grid).cell(1, row));
        assertEquals("10.00 €", test(grid).cell(2, row));
        assertEquals("10", test(grid).cell(4, row));

        // WHEN: Searching for the book
        test($(FilterField.class).id("filter-field")).setValue("Filter book");

        // THEN: The book is found at the first row and the row contains the new
        // book
        assertEquals(1, test(grid).size());
        assertEquals("Filter book", test(grid).cell(1, 0));
        assertEquals("10.00 €", test(grid).cell(2, 0));
        assertEquals("10", test(grid).cell(4, 0));

        // Cleanup
        ui.getProductService().deleteProduct(test(grid).item(0).getId());
    }

    @Test
    public void filling_new_product_and_cancelling_it_will_show_confirm_dialog() {
        // WHEN: Opening empty product form
        test($(view, Button.class).id("new-product")).click();

        // THEN: Form is shown
        assertTrue(form.isShown());
        assertFalse($(view, Button.class).id("new-product").isEnabled());

        // WHEN: Filling the form and canceling it
        test($(form, TextField.class).id("product-name")).setValue("New book");
        test($(form, TextField.class).id("price")).setValue("10.0 €");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);
        test($(form, NumberField.class).id("stock-count")).setValue(10);

        test($(form, Button.class).caption("Cancel").single()).click();

        // THEN: Confirm dialog is shown
        var dialog = $(Window.class).id("confirm-dialog");

        // WHEN: Confirming the cancel
        test($(dialog, Button.class).id("confirm-button")).click();

        // THEN: Form is closed and grid is focused
        assertFalse(form.isShown());
        assertTrue(test(grid).isFocused());
        assertTrue($(view, Button.class).id("new-product").isEnabled());
    }

    @Test
    public void opening_empty_product_form_can_be_cancelled_without_confirmation() {
        // WHEN: Opening empty product form
        test($(view, Button.class).id("new-product")).click();
        assertFalse($(view, Button.class).id("new-product").isEnabled());

        // THEN: Form is shown
        assertTrue(form.isShown());

        // WHEN: Canceling the form
        test($(form, Button.class).caption("Cancel").single()).click();

        // THEN: Form is closed and grid is focused
        assertFalse(form.isShown());
        assertTrue(test(grid).isFocused());
        assertTrue($(view, Button.class).id("new-product").isEnabled());
    }

    @Test
    public void delete_product_requires_confirming() {
        // GIVEN: A book
        createBook("Delete book");

        // WHEN: Searching for the book
        test($(FilterField.class).id("filter-field")).setValue("Delete book");

        // THEN: The book is found at the first row
        var row = 0;
        var book = test(grid).item(row);
        var id = book.getId();
        assertEquals("Delete book", book.getProductName());

        // WHEN: Deleting the book
        test(grid).click(1, row);
        assertFalse($(view, Button.class).id("new-product").isEnabled());
        test($(form, Button.class).id("delete-button")).click();

        // THEN: Confirm dialog is shown
        var dialog = $(Window.class).id("confirm-dialog");

        // WHEN: Confirming the delete
        test($(dialog, Button.class).id("confirm-button")).click();

        // THEN: Notification is shown with the book name, form is closed, book
        // is deleted and grid is empty
        assertNotification(
                String.format("\"%s\" removed", book.getProductName()));

        assertFalse(form.isShown());
        assertEquals(null, ui.getProductService().getProductById(id));
        assertEquals(0, test(grid).size());
        assertTrue($(view, Button.class).id("new-product").isEnabled());
    }

    @SuppressWarnings("unchecked")
    private void createBook(String name) {
        test($(view, Button.class).id("new-product")).click();
        test($(TextField.class).id("product-name")).setValue(name);
        test($(AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);
        test($(TextField.class).id("price")).setValue("35.0 €");
        var categories = VaadinCreateUI.get().getProductService()
                .getAllCategories().stream().toList();
        test($(CheckBoxGroup.class).id("category"))
                .clickItem(categories.get(1));
        test($(CheckBoxGroup.class).id("category"))
                .clickItem(categories.get(2));
        test($(NumberField.class).id("stock-count")).setValue(100);
        test($(Button.class).id("save-button")).click();
    }

    @Test
    public void attempting_to_open_product_deleted_concurrently_by_other_user_will_show_error() {
        // GIVEN: A book
        createBook("Concurrent delete");

        // WHEN: Searching for the book
        test($(FilterField.class).id("filter-field"))
                .setValue("Concurrent delete");

        // THEN: The book is found at the first row
        var book = test(grid).item(0);

        // WHEN: Simulating other user deleting the book
        ui.getProductService().deleteProduct(book.getId());

        // WHEN: Clicking the row
        test(grid).click(1, 0);

        // THEN: Error notification is shown and form is not opened and grid is
        // empty
        assertEquals("Product was deleted.",
                $(Notification.class).last().getCaption());
        assertFalse(form.isShown());
        assertTrue($(view, Button.class).id("new-product").isEnabled());

        assertEquals(0, test(grid).size());
    }

    @Test
    public void editing_product_and_saving_it_will_show_it_highlighted_in_grid() {
        // GIVEN: A book
        createBook("Test book");

        // WHEN: Searching for the book
        test($(FilterField.class).id("filter-field")).setValue("Test book");

        // THEN: The book is found at the first row
        var row = 0;
        var book = test(grid).item(row);

        // WHEN: Clicking the first row
        test(grid).click(1, row);

        // THEN: Form is shown
        assertTrue(form.isShown());

        var id = book.getId();
        assertNotNull(id);

        // WHEN: Editing the book and saving it
        test($(form, TextField.class).id("product-name"))
                .setValue("Edited book");
        test($(form, Button.class).id("save-button")).click();

        // THEN: Form is closed
        assertFalse(form.isShown());

        var edited = ui.getProductService().getProductById(id);

        // WHEN: Searching for the edited book
        test($(FilterField.class).id("filter-field")).setValue("Edited book");

        // THEN: The edited book is found at the first row and the row is
        // highlighted
        var name = (String) test(grid).cell(1, row);
        assertEquals("Edited book", name);
        assertEquals("Edited book", edited.getProductName());
        assertEquals(VaadinCreateTheme.BOOKVIEW_GRID_EDITED,
                test(grid).styleName(0));

        // FINALLY: Delete the book to avoid side effects
        ui.getProductService().deleteProduct(id);
    }

    @Test
    public void clicking_cancel_button_in_form_having_changes_will_show_confirm_dialog_and_confirming_cancel_will_close_form_and_unlock_product_and_return_focus_to_grid() {
        var book = test(grid).item(0);

        // WHEN: Opening product from the first row and changing the product
        test(grid).click(1, 0);
        assertTrue(form.isShown());
        assertFalse($(view, Button.class).id("new-product").isEnabled());

        test($(form, TextField.class).id("product-name"))
                .setValue("Edited book");

        // WHEN: Clicking cancel button
        test($(form, Button.class).caption("Cancel").single()).click();

        // THEN: Edited field is marked as dirty and has the original value is
        // shown in the description and the product is still locked and confirm
        // dialog is shown
        assertTrue($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertTrue($(form, TextField.class).id("product-name").getDescription()
                .contains(book.getProductName()));
        assertNotNull(LockedObjects.get().isLocked(book));

        var dialog = $(Window.class).id("confirm-dialog");

        // WHEN: Clicking confirm button on the confirm dialog
        test($(dialog, Button.class).id("confirm-button")).click();

        // THEN: The form is closed and the product is unlocked and the grid is
        // focused
        assertFalse(form.isShown());
        assertTrue(test(grid).isFocused());
        assertTrue($(view, Button.class).id("new-product").isEnabled());

        assertNull(LockedObjects.get().isLocked(book));
    }

    @Test
    public void when_clicking_another_row_when_opened_form_has_changes_confirm_dialog_is_shown_and_field_is_marked_dirty_confirming_dialog_will_close_the_form_and_reopening_form_will_not_have_field_dirty() {
        var book = test(grid).item(0);

        // WHEN: Opening product from the first row
        test(grid).click(1, 0);

        // THEN: Form is shown
        assertTrue(form.isShown());
        assertFalse($(view, Button.class).id("new-product").isEnabled());

        // WHEN: Changing the product and clicking an another product
        test($(form, TextField.class).id("product-name"))
                .setValue("Changed book");
        test(grid).click(1, 1);

        // THEN: The edited field is marked as dirty and has the original value
        // is shown in the description and the confirm dialog is shown
        assertTrue($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertTrue($(form, TextField.class).id("product-name").getDescription()
                .contains(book.getProductName()));
        var dialog = $(Window.class).id("confirm-dialog");

        // WHEN: Clicking confirm button on the confirm dialog
        test($(dialog, Button.class).id("confirm-button")).click();

        // THEN: The form is closed
        assertFalse(form.isShown());
        assertTrue($(view, Button.class).id("new-product").isEnabled());

        // WHEN: Opening the first row again
        test(grid).click(1, 0);

        // THEN: The form is shown and the field is no longer marked as dirty
        assertTrue(form.isShown());
        assertFalse($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertNull(
                $(form, TextField.class).id("product-name").getDescription());

        // FINALLY: Close form gracefully to avoid side effects
        test($(form, Button.class).id("cancel-button")).click();
        assertFalse(form.isShown());
        assertTrue($(view, Button.class).id("new-product").isEnabled());
    }

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
    public void clicking_other_row_when_form_has_edits_will_show_confirm_dialog_and_edited_field_is_marked_dirty_clicking_discard_will_revert_edits_and_field_is_no_longer_dirty() {
        var book = test(grid).item(0);
        var productName = book.getProductName();

        // WHEN: Opening product from the first row and changing the product
        test(grid).click(1, 0);
        assertTrue(form.isShown());
        assertFalse($(view, Button.class).id("new-product").isEnabled());

        test($(form, TextField.class).id("product-name"))
                .setValue("Changed book");

        // WHEN: Clicking an another product
        test(grid).click(1, 1);

        // THEN: The edited field is marked as dirty and has the original value
        // is shown in the description and the confirm dialog is shown
        assertTrue($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertTrue($(form, TextField.class).id("product-name").getDescription()
                .contains(book.getProductName()));
        var dialog = $(Window.class).id("confirm-dialog");

        // WHEN: Clicking cancel button on the confirm dialog
        test($(dialog, Button.class).id("cancel-button")).click();

        // THEN: The form is still shown and the field is still marked as dirty
        assertTrue(form.isShown());
        assertTrue($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertTrue($(form, TextField.class).id("product-name").getDescription()
                .contains(book.getProductName()));

        assertEquals(book, grid.getSelectedRow());

        // WHEN: Clicking the discard button
        test($(form, Button.class).id("discard-button")).click();

        // THEN: The field is no longer marked as dirty and the description is
        // not shown and the original value is shown in the field
        assertFalse($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertNull(
                $(form, TextField.class).id("product-name").getDescription());
        assertEquals(productName,
                $(form, TextField.class).id("product-name").getValue());

        // WHEN: Clicking cancel button
        test($(form, Button.class).id("cancel-button")).click();

        // THEN: The form is closed
        assertFalse(form.isShown());
        assertTrue($(view, Button.class).id("new-product").isEnabled());
    }

    @Test
    public void opening_product_changed_by_other_user_is_showing_the_changes_made_by_him() {
        var book = test(grid).item(0);

        // WHEN: Other user is persisting a change to the database
        var edited = ui.getProductService().getProductById(book.getId());
        var productName = edited.getProductName();
        edited.setProductName("Touched book");
        var saved = ui.getProductService().updateProduct(edited);

        // WHEN: Opening the product by the first user
        test(grid).click(1, 0);

        // THEN: Form is shown and product name is upto date in the form and in
        // the grid
        assertTrue(form.isShown());
        assertEquals("Touched book",
                $(form, TextField.class).id("product-name").getValue());

        var name = (String) test(grid).cell(1, 0);
        assertEquals("Touched book", name);

        // WHEN: Clicking cancel button
        test($(form, Button.class).id("cancel-button")).click();

        // THEN: Form is closed
        assertFalse(form.isShown());

        // FINALLY: Reset the name back to original in order to avoid side
        // effects
        edited.setProductName(productName);
        ui.getProductService().updateProduct(saved);
    }

    @Test
    public void it_is_not_possible_to_open_locked_row_and_grid_shows_locked_by_tooltip() {
        // GIVEN: A locked book
        var book = test(grid).item(15);
        LockedObjects.get().lock(book, CurrentUser.get().get());

        // THEN: The row has the locked by tooltip
        assertEquals("Edited by Admin", test(grid).description(15));

        // WHEN: Clicking the row
        test(grid).click(1, 15);

        // THEN: Form is not shown
        assertFalse(form.isShown());
        assertTrue($(view, Button.class).id("new-product").isEnabled());

        // FINALLY: Unlock the book
        LockedObjects.get().unlock(book);
    }

    @Test
    public void opening_product_form_will_lock_the_product_and_locked_product_will_be_unlocked_when_product_in_form_is_changed() {
        // GIVEN: The book from the first row is not locked
        var book = test(grid).item(0);
        assertNull(LockedObjects.get().isLocked(book));

        // WHEN: Opening product from the first row
        test(grid).click(1, 0);

        // THEN: Form is shown and book is locked
        assertTrue(form.isShown());
        assertNotNull(LockedObjects.get().isLocked(book));

        // WHEN: Clicking the second row
        test(grid).click(1, 1);

        // THEN: Form is shown and the book from the first row is not locked and
        // the book from the second row is locked
        assertTrue(form.isShown());
        assertNull(LockedObjects.get().isLocked(book));
        assertNotNull(LockedObjects.get().isLocked(test(grid).item(1)));

        // WHEN: Clicking the second row again
        test(grid).click(1, 1);

        // THEN: Form is closed and the book from the second row is not locked
        assertFalse(form.isShown());
        assertNull(LockedObjects.get().isLocked(test(grid).item(1)));
        assertTrue($(view, Button.class).id("new-product").isEnabled());
    }

    @Test
    public void product_is_locked_when_opened_and_when_navigating_to_other_view_it_is_unlocked() {
        // GIVEN: The book from the first row is not locked
        var book = test(grid).item(0);
        assertNull(LockedObjects.get().isLocked(book));

        // WHEN: Clicking the first row
        test(grid).click(1, 0);

        // THEN: Form is shown and book is locked
        assertTrue(form.isShown());
        assertNotNull(LockedObjects.get().isLocked(book));

        // WHEN: Navigating to another view
        $(MenuButton.class).caption("About").single().click();

        // THEN: The book is unlocked
        assertNull(LockedObjects.get().isLocked(book));
    }

    @Test
    public void clicking_another_row_when_form_with_changes_is_open_will_show_confirm_dialog() {
        // WHEN: Opening product from the first row and changing the product
        test(grid).click(1, 0);

        test($(form, TextField.class).id("product-name"))
                .setValue("Edited book");
        test($(form, NumberField.class).id("stock-count")).setValue(100);

        // WHEN: Clicking an another product
        test(grid).click(1, 1);

        // THEN: Confirm dialog is shown
        var dialog = $(Window.class).id("confirm-dialog");

        // WHEN: Clicking confirm button
        test($(dialog, Button.class).id("confirm-button")).click();

        // THEN: The form is closed and row is not marked as locked
        assertFalse(form.isShown());
        assertFalse(test(grid).styleName(0)
                .contains(VaadinCreateTheme.BOOKVIEW_GRID_LOCKED));
        assertTrue($(view, Button.class).id("new-product").isEnabled());
    }

    @Test
    public void editing_product_and_saving_changes_are_shown_in_grid() {
        // WHEN: Opening product from the first row and changing the product and
        // clicking save button
        test(grid).click(1, 0);

        var name = $(form, TextField.class).id("product-name").getValue();
        test($(form, TextField.class).id("product-name"))
                .setValue("Different book");

        test($(form, Button.class).id("save-button")).click();

        // THEN: Notification is shown with the book name, form is closed
        assertNotification("\"Different book\" updated");
        assertFalse(form.isShown());
        assertTrue($(view, Button.class).id("new-product").isEnabled());

        // THEN: The edited row is highlighted and has the new name
        assertTrue(test(grid).styleName(0)
                .contains(VaadinCreateTheme.BOOKVIEW_GRID_EDITED));
        assertEquals("Different book", test(grid).cell(1, 0));

        // FINALLY: Reset the name back to original in order to avoid side
        // effects
        test(grid).click(1, 0);
        test($(form, TextField.class).id("product-name")).setValue(name);

        test($(form, Button.class).id("save-button")).click();
        assertEquals(name, test(grid).cell(1, 0));
        assertTrue($(view, Button.class).id("new-product").isEnabled());
    }

    @Test
    public void changing_product_and_reverting_edits_manually_by_editing_will_not_keep_form_dirty() {
        // WHEN: Opening product from the first row and changing the product
        test(grid).click(1, 0);

        var name = $(form, TextField.class).id("product-name").getValue();
        var count = $(form, NumberField.class).id("stock-count").getValue();
        test($(form, TextField.class).id("product-name"))
                .setValue("Edited book");
        test($(form, NumberField.class).id("stock-count")).setValue(100);

        // THEN: Save and discard buttons are enabled
        assertTrue($(form, Button.class).id("save-button").isEnabled());
        assertTrue($(form, Button.class).id("discard-button").isEnabled());

        // WHEN: Reverting the changes
        test($(form, TextField.class).id("product-name")).setValue(name);
        test($(form, NumberField.class).id("stock-count")).setValue(count);

        // THEN: Save and discard buttons are disabled
        assertFalse($(form, Button.class).id("save-button").isEnabled());
        assertFalse($(form, Button.class).id("discard-button").isEnabled());

        // WHEN: Clicking an another product
        test(grid).click(1, 1);

        // THEN: Form is shown and the fields are updated with the new product
        assertTrue(form.isShown());
        assertFalse($(view, Button.class).id("new-product").isEnabled());
        assertEquals($(form, TextField.class).id("product-name").getValue(),
                test(grid).cell(1, 1));

        // FINALLY: Close form gracefully to avoid side effects
        test($(form, Button.class).id("cancel-button")).click();
        assertFalse(form.isShown());
        assertTrue($(view, Button.class).id("new-product").isEnabled());
    }

    @Test
    public void confirm_dialog_is_shown_when_menu_button_is_clicked_if_product_has_changes() {
        // WHEN: Opening product from the first row and changing the product
        test(grid).click(1, 0);

        test($(form, TextField.class).id("product-name"))
                .setValue("Edited book");
        test($(form, NumberField.class).id("stock-count")).setValue(100);

        // WHEN: Clicking about button from the menu
        $(MenuButton.class).caption("About").single().click();

        // THEN: Confirm dialog is shown
        var dialog = $(Window.class).id("confirm-dialog");

        // WHEN: Clicking confirm button
        test($(dialog, Button.class).id("confirm-button")).click();

        // THEN: The form is closed and the about view is shown and book view is
        // not shown
        assertFalse(form.isShown());

        assertEquals(1, $(AboutView.class).size());
        assertEquals(0, $(BooksView.class).size());
    }

    @Test
    public void confirm_dialog_is_shown_when_logout_button_is_clicked_if_product_has_changes() {
        // WHEN: Opening product from the first row and changing the product
        test(grid).click(1, 0);

        test($(form, TextField.class).id("product-name"))
                .setValue("Edited book");
        test($(form, NumberField.class).id("stock-count")).setValue(100);

        // WHEN: Clicking logout button
        logout();

        // THEN: Confirm dialog is shown and book view is still open and form is
        // still shown
        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("cancel-button")).click();

        assertEquals(1, $(BooksView.class).size());
        assertTrue(form.isShown());

        // FINALLY: Close form gracefully to avoid side effects
        test($(form, Button.class).id("discard-button")).click();
        test($(form, Button.class).id("cancel-button")).click();
        assertFalse(form.isShown());
        assertTrue($(view, Button.class).id("new-product").isEnabled());
    }

    @Test
    public void validation_is_shown_when_product_name_is_empty() {
        // WHEN: Opening product from the first row and clearing the product
        // name and focusing on stock count
        test(grid).click(1, 0);

        test($(form, TextField.class).id("product-name")).setValue("");
        test($(form, NumberField.class).id("stock-count")).focus();

        // THEN: Validation error is shown in product name field and the field
        // is invalid
        assertTrue(
                test($(form, TextField.class).id("product-name")).isInvalid());
        assertEquals(
                "Product name must have at least two characters and maximum of 100",
                test($(form, TextField.class).id("product-name"))
                        .errorMessage());

        // WHEN: Clicking discard button
        test($(form, Button.class).id("discard-button")).click();

        // THEN: Clicking discard button closes the form without showing the
        // confirm dialog
        test($(form, Button.class).id("cancel-button")).click();
        assertFalse(form.isShown());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void when_saving_product_with_concurrently_deleted_categoty_error_is_shown() {
        // GIVEN: A category
        var category = new Category();
        category.setName("Science");
        category = ui.getProductService().updateCategory(category);
        assertNotNull(category.getId());

        // WHEN: Opening product from the first row
        test(grid).click(1, 0);

        // WHEN: Simulating other user deleting the category while editor is
        // open
        ui.getProductService().deleteCategory(category.getId());

        // WHEN: Using the category in the product and saving the product
        test($(form, CheckBoxGroup.class).id("category")).clickItem(category);
        test($(form, Button.class).id("save-button")).click();

        // THEN: Error is shown
        assertEquals("One or more of the selected categories were deleted.",
                $(Notification.class).last().getCaption());

        // WHEN: Clicking cancel button
        test($(form, Button.class).id("cancel-button")).click();

        // THEN: Form is closed
        assertFalse(form.isShown());
    }

    @Test
    @SuppressWarnings({ "unchecked", "java:S5961" })
    public void tooltip_in_narrow_mode_and_edited_field_has_content_sanitized_to_prevent_xss() {
        // WHEN: Making window small in order to show tooltip
        ui.getPage().updateBrowserWindowSize(500, 1024, true);

        // WHEN: Creating a book with offending content
        test($(view, Button.class).id("new-product")).click();
        test($(form, TextField.class).id("product-name")).setValue(
                "<b><img src=1 onerror=alert(document.domain)>A new book</b>");
        test($(form, TextField.class).id("price")).setValue("10.0 €");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);
        test($(form, NumberField.class).id("stock-count")).setValue(10);

        var cat = ui.getProductService().getAllCategories().stream().findFirst()
                .get();
        test($(form, CheckBoxGroup.class).id("category")).clickItem(cat);

        // WHEN: Clicking save button
        test($(form, Button.class).id("save-button")).click();

        // THEN: Form is closed and the product is on the last row
        assertFalse(form.isShown());

        int row = test(grid).size() - 1;

        // THEN: The JS sanitized away and the text content remain
        assertFalse(test(grid).description(row).contains("alert"));
        assertTrue(test(grid).description(row).contains("A new book"));

        // WHEN: Clicking the row
        test(grid).click(1, row);
        var id = test(grid).item(row).getId();
        assertNotNull(id);

        // THEN: Form is shown
        assertTrue(form.isShown());

        // WHEN: Editing the book and clicking cancel button
        test($(form, TextField.class).id("product-name"))
                .setValue("The new book");
        test($(form, Button.class).id("cancel-button")).click();

        // THEN: Confirm dialog is shown
        var dialog = $(Window.class).id("confirm-dialog");

        // WHEN: Canceling the cancel operation
        test($(dialog, Button.class).id("cancel-button")).click();

        // THEN: Form is still shown and field is dirty and has tooltip with the
        // original content where JS is sanitized and text content remain
        assertTrue($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertFalse($(form, TextField.class).id("product-name").getDescription()
                .contains("alert"));
        assertTrue($(form, TextField.class).id("product-name").getDescription()
                .contains("A new book"));

        // WHEN: Clicking discard button
        test($(form, Button.class).id("discard-button")).click();

        // THEN: Cancel button is clicked and form is closed without showing the
        // confirm dialog
        test($(form, Button.class).id("cancel-button")).click();
        assertFalse(form.isShown());

        // Cleanup
        ui.getProductService().deleteProduct(id);
    }

    @Test
    public void save_event_received_by_presenter_refreshes_grid() {
        // GIVEN: Create presenter simulating other user
        var presenter = createBooksPresenter();
        // GIVEN: Book on the first row
        var book = test(grid).item(0);

        // WHEN: Opening the book on the second row
        test(grid).click(1, 1);

        // THEN: Form is shown
        assertTrue(form.isShown());

        // WHEN: Save item in the other presenter, that fires event catched by
        // this view
        var name = book.getProductName();
        book.setProductName("Book to be refreshed");
        var saved = presenter.saveProduct(book);

        // THEN: That item was properly refreshed in the event and form is not
        // closed
        assertEquals("Book to be refreshed", test(grid).cell(1, 0));
        assertTrue(form.isShown());

        // Cleanup
        saved.setProductName(name);
        ui.getProductService().updateProduct(saved);
    }

    private BooksPresenter createBooksPresenter() {
        var bookView = new BooksView();
        // It is not possible to have multiple parallel UIs in this thread
        view.addComponent(bookView);
        var presenter = new BooksPresenter(bookView);
        presenter.requestUpdateProducts();
        var fake = $(bookView, FakeGrid.class).first();
        waitWhile(fake, f -> f.isVisible(), 10);
        return presenter;
    }

    @Test
    public void searching_product_with_no_match_will_show_no_matches_and_adding_product_with_that_name_will_remove_no_matches_and_deleting_the_product_will_show_no_matches_again() {
        // WHEN: Searching book that does not exists
        test($(FilterField.class).id("filter-field")).setValue("No match");

        // THEN: rGid is empty and no matches label is shown
        assertEquals(0, test(grid).size());
        assertTrue($(NoMatches.class)
                .styleName(VaadinCreateTheme.BOOKVIEW_NOMATCHES).single()
                .isVisible());

        // WHEN: Adding a book with matching name
        test($(view, Button.class).id("new-product")).click();
        test($(form, TextField.class).id("product-name"))
                .setValue("No match book");
        test($(form, TextField.class).id("price")).setValue("10.0 €");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);
        test($(form, NumberField.class).id("stock-count")).setValue(10);
        test($(form, Button.class).id("save-button")).click();

        // THEN: Grid has one item and no matches label is hidden
        assertEquals(1, test(grid).size());
        assertFalse($(NoMatches.class)
                .styleName(VaadinCreateTheme.BOOKVIEW_NOMATCHES).single()
                .isVisible());

        // WHEN: Deleting the book
        test(grid).click(1, 0);
        test($(form, Button.class).id("delete-button")).click();
        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        // THEN: Grid is empty and no matches label is shown
        assertEquals(0, test(grid).size());
        // Assert that no matches label is shown again
        assertTrue($(NoMatches.class)
                .styleName(VaadinCreateTheme.BOOKVIEW_NOMATCHES).single()
                .isVisible());
    }

    @SuppressWarnings("java:S5961")
    @Test
    public void different_columns_are_shown_based_on_browser_window_width() {
        // WHEN: Making window large
        ui.getPage().updateBrowserWindowSize(1600, 1024, true);

        // THEN: All columns are shown and no description is shown
        grid.getColumns().forEach(col -> {
            assertFalse(col.isHidden());
        });
        assertEquals(null, test(grid).description(0));

        // WHEN: Making window smaller
        ui.getPage().updateBrowserWindowSize(1200, 1024, true);

        // THEN: First and last columns are hidden and no description is shown
        assertTrue(grid.getColumns().get(0).isHidden());
        assertFalse(grid.getColumns().get(1).isHidden());
        assertFalse(grid.getColumns().get(2).isHidden());
        assertFalse(grid.getColumns().get(3).isHidden());
        assertFalse(grid.getColumns().get(4).isHidden());
        assertTrue(grid.getColumns().get(5).isHidden());

        assertEquals(null, test(grid).description(0));

        // WHEN: Making window even smaller
        ui.getPage().updateBrowserWindowSize(900, 1024, true);

        // THEN: First, fith and last columns are hidden and description is
        // still not shown
        assertTrue(grid.getColumns().get(0).isHidden());
        assertFalse(grid.getColumns().get(1).isHidden());
        assertFalse(grid.getColumns().get(2).isHidden());
        assertFalse(grid.getColumns().get(3).isHidden());
        assertTrue(grid.getColumns().get(4).isHidden());
        assertTrue(grid.getColumns().get(5).isHidden());

        assertEquals(null, test(grid).description(0));

        // WHEN: Making window even smaller
        ui.getPage().updateBrowserWindowSize(500, 1024, true);

        // THEN: Only second and third columns are shown and description is
        // shown
        assertTrue(grid.getColumns().get(0).isHidden());
        assertFalse(grid.getColumns().get(1).isHidden());
        assertFalse(grid.getColumns().get(2).isHidden());
        assertTrue(grid.getColumns().get(3).isHidden());
        assertTrue(grid.getColumns().get(4).isHidden());
        assertTrue(grid.getColumns().get(5).isHidden());

        var doc = Jsoup.parse(test(grid).description(0));
        var book = test(grid).item(0);
        assertEquals(book.getProductName(),
                doc.getElementsByTag("b").get(0).text());
        assertEquals(1, doc.getElementsByClass("v-icon").size());
    }

    @Test
    public void clicking_sorting_grid_by_price_will_sort_ascending_second_click_descending() {
        int size = test(grid).size();

        // WHEN: Clicking price column sorting toggle
        test(grid).toggleColumnSorting(2);

        // THEN: Grid is sorted by price in ascending order
        for (int i = 1; i < size; i++) {
            var result = test(grid).item(i - 1).getPrice()
                    .compareTo(test(grid).item(i).getPrice());
            assertTrue(result <= 0);
        }

        // WHEN: Clicking price column sorting toggle again
        test(grid).toggleColumnSorting(2);

        // THEN: Grid is sorted by price in descending order
        for (int i = 1; i < size; i++) {
            var result = test(grid).item(i - 1).getPrice()
                    .compareTo(test(grid).item(i).getPrice());
            assertTrue(result >= 0);
        }
    }

    @Test
    public void clicking_sorting_grid_by_name_will_sort_ascending_second_click_descending() {
        int size = test(grid).size();

        // WHEN: Clicking name column sorting toggle
        test(grid).toggleColumnSorting(1);

        // THEN: Grid is sorted by name in alphabetically ascending order
        for (int i = 1; i < size; i++) {
            var result = ((String) test(grid).cell(1, i - 1))
                    .compareToIgnoreCase((String) test(grid).cell(1, i));
            assertTrue(result <= 0);
        }

        // WHEN: Clicking name column sorting toggle again
        test(grid).toggleColumnSorting(1);

        // THEN: Grid is sorted by name in alphabetically descending order
        for (int i = 1; i < size; i++) {
            var result = ((String) test(grid).cell(1, i - 1))
                    .compareToIgnoreCase((String) test(grid).cell(1, i));
            assertTrue(result >= 0);
        }
    }

    @Test
    @SuppressWarnings({ "unused", "java:S2699" })
    public void book_view_is_serializable()
            throws IOException, ClassNotFoundException {
        var bs = new ByteArrayOutputStream();
        var os = new ObjectOutputStream(bs);
        os.writeObject(ui.getSession());
        os.flush();
        os.close();

        var a = bs.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(a);
        ObjectInputStream in = new ObjectInputStream(bis);
        var v = (VaadinSession) in.readObject();
    }

}
