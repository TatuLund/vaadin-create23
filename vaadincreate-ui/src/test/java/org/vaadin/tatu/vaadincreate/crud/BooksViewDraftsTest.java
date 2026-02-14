package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.about.AboutView;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.common.NumberField;
import org.vaadin.tatu.vaadincreate.crud.form.AvailabilitySelector;
import org.vaadin.tatu.vaadincreate.crud.form.BookForm;

import com.vaadin.server.ServiceException;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class BooksViewDraftsTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private BookGrid grid;
    private BookForm form;
    private BooksView view;
    private VerticalLayout layout;
    private ProductDataService service;
    private User user;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        service = ui.getProductService();

        mockVaadin(ui);
        login();

        view = navigate(BooksView.VIEW_NAME, BooksView.class);

        layout = $(view, VerticalLayout.class).first();
        grid = $(layout, BookGrid.class).single();
        waitForGrid(layout, grid);
        form = $(view, BookForm.class).single();
        user = CurrentUser.get().get();
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
    }

    @Test
    @SuppressWarnings("java:S5961")
    public void closing_session_with_unsaved_changes_in_existing_product_will_automatically_save_draft_and_after_relogin_continuing_with_draft_dialog_is_shown_upon_confirming_changes_can_be_saved()
            throws ServiceException {
        // GIVEN: A book is created
        createBook("Draft book");

        // WHEN: Searching for the book
        test($(FilterField.class).id("filter-field")).setValue("Draft book");

        // THEN: The book is found on the first row of the grid and the
        // form is
        // shown
        test(grid).click(1, 0);
        var book = test(grid).item(0);
        var id = book.getId();
        assertTrue(form.isShown());

        // WHEN: Book is modified and session is closed
        test($(form, TextField.class).id("product-name"))
                .setValue("Modified book");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);

        // This will close the ui by force, same as closing browser
        tearDown();

        // THEN: A draft is saved
        assertNotNull(service.findDraft(user));

        // WHEN: Starting the UI again and logging in
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        // THEN: A dialog is shown to continue with the draft
        var dialog = $(Window.class).id("confirm-dialog");

        // WHEN: Confirming to edit draft
        test($(dialog, Button.class).id("confirm-button")).click();

        // THEN: Books view is shown with the form open
        grid = $(BookGrid.class).single();
        waitForGrid((CssLayout) grid.getParent(), grid);
        form = $(BookForm.class).single();
        assertTrue(form.isShown());

        // THEN: Draft is removed from the database
        assertNull(service.findDraft(user));

        // THEN: The form is populated with the draft data and the form
        // is marked as dirty and the original values are shown in the
        // descriptions
        assertEquals("Modified book",
                $(form, TextField.class).id("product-name").getValue());
        assertTrue($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertTrue($(form, TextField.class).id("product-name").getDescription()
                .contains(book.getProductName()));

        assertEquals(Integer.valueOf(0),
                $(form, NumberField.class).id("stock-count").getValue());
        assertFalse($(form, NumberField.class).id("stock-count").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));

        assertEquals(Availability.AVAILABLE, $(form, AvailabilitySelector.class)
                .id("availability").getValue());
        assertTrue($(form, AvailabilitySelector.class).id("availability")
                .getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertTrue($(form, AvailabilitySelector.class).id("availability")
                .getDescription().contains(book.getAvailability().toString()));

        assertEquals(book.getCategory(),
                $(form, CheckBoxGroup.class).id("category").getValue());
        assertFalse($(form, CheckBoxGroup.class).id("category").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertFalse($(form, TextField.class).id("price").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));

        // WHEN: Modifying the draft to be valid and clicking save
        // button
        test($(form, NumberField.class).id("stock-count")).setValue(200);

        test($(form, Button.class).id("save-button")).click();

        assertNotificationForUpdatedBook();

        assertFalse(form.isShown());

        // Clean up the book created for the test case to avoid
        // conflicts
        ui.getProductService().deleteProduct(id);
    }

    @Test
    public void closing_session_with_unsaved_changes_of_new_product_will_save_draft_and_upon_relogin_draft_is_merged_with_new_product_and_can_be_saved_successfully()
            throws ServiceException {
        // WHEN: Clicking the new product button
        test($(view, Button.class).id("new-product")).click();

        // THEN: The form is shown
        assertTrue(form.isShown());

        // WHEN: Enterong partial data (not valid) in the new product
        // and
        // closing the session
        test($(form, TextField.class).id("product-name"))
                .setValue("Modified book");
        test($(form, NumberField.class).id("stock-count")).setValue(200);
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.DISCONTINUED);

        // This will close the ui by force, same as closing browser
        tearDown();

        // THEN: A draft is saved
        assertNotNull(service.findDraft(user));

        // WHEN: Starting the UI again and logging in
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        // THEN: A dialog is shown to continue with the draft
        var dialog = $(Window.class).id("confirm-dialog");

        // WHEN: Confirming to edit draft
        test($(dialog, Button.class).id("confirm-button")).click();

        // THEN: Books view is shown with the form open and draft is
        // removed
        grid = $(BookGrid.class).single();
        waitForGrid((CssLayout) grid.getParent(), grid);
        form = $(BookForm.class).single();
        assertTrue(form.isShown());
        assertNull(service.findDraft(user));

        // THEN: The empty form is merged with the draft data and the
        // form is
        // marked as dirty
        assertEquals("Modified book",
                $(form, TextField.class).id("product-name").getValue());
        assertTrue($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));

        assertEquals(Integer.valueOf(200),
                $(form, NumberField.class).id("stock-count").getValue());
        assertTrue($(form, NumberField.class).id("stock-count").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));

        assertEquals(Availability.DISCONTINUED,
                $(form, AvailabilitySelector.class).id("availability")
                        .getValue());
        assertTrue($(form, AvailabilitySelector.class).id("availability")
                .getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));

        assertFalse($(form, CheckBoxGroup.class).id("category").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertFalse($(form, TextField.class).id("price").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));

        assertTrue($(form, CheckBoxGroup.class).id("category").getValue()
                .isEmpty());

        // WHEN: Modifying the draft to be valid and clicking save
        // button
        test($(form, NumberField.class).id("stock-count")).setValue(0);

        test($(form, Button.class).id("save-button")).click();

        // THEN: A notification is shown and the form is closed and the
        // book is saved and appears in the grid on the last row
        assertNotificationForUpdatedBook();
        assertFalse(form.isShown());
        var book = test(grid).item(test(grid).size() - 1);
        assertEquals("Modified book", book.getProductName());

        // Clean up the book created for the test case to avoid
        // conflicts
        var id = book.getId();
        ui.getProductService().deleteProduct(id);
    }

    private void assertNotificationForUpdatedBook() {
        assertNotification("\"Modified book\" updated");
    }

    @Test
    @SuppressWarnings("java:S5961")
    public void closing_session_with_unsaved_changes_will_save_draft_and_after_other_user_has_changed_original_product_and_after_relogin_draft_is_merged_with_changes_made_by_other_user_and_result_can_be_saved_succesfully()
            throws ServiceException {
        // GIVEN: a book is created
        createBook("Draft book");

        // WHEN:searching for the book
        test($(FilterField.class).id("filter-field")).setValue("Draft book");

        // THEN: the book is found on the first row of the grid and the
        // form is shown
        test(grid).click(1, 0);
        var book = test(grid).item(0);
        assertTrue(form.isShown());

        // WHEN: book is modified and session is closed
        test($(form, TextField.class).id("product-name"))
                .setValue("Modified book");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);

        // This will close the ui by force, same as closing browser
        tearDown();

        // THEN: a draft is saved
        assertNotNull(service.findDraft(user));

        // WHEN: Simulating other user editing books stock count and
        // starting
        // the UI again
        var edited = service.getProductById(book.getId());
        edited.setProductName("Edited book");
        edited.setStockCount(300);
        service.updateProduct(edited);

        // Start again
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        // THEN: a dialog is shown to continue with the draft
        var dialog = $(Window.class).id("confirm-dialog");

        // WHEN: confirming to edit draft
        test($(dialog, Button.class).id("confirm-button")).click();

        // THEN: books view is shown with the form open and draft is
        // removed
        // from the database
        grid = $(BookGrid.class).single();
        waitForGrid((CssLayout) grid.getParent(), grid);
        form = $(BookForm.class).single();
        assertTrue(form.isShown());
        assertNull(service.findDraft(user));

        // THEN: the form is merged with the draft data and the form is
        // marked as dirty
        assertEquals("Modified book",
                $(form, TextField.class).id("product-name").getValue());
        assertTrue($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertTrue($(form, TextField.class).id("product-name").getDescription()
                .contains("Edited book"));

        // THEN: The orignal stock count shown in the description is the
        // one by
        // the other user
        assertEquals(Integer.valueOf(0),
                $(form, NumberField.class).id("stock-count").getValue());
        assertTrue($(form, NumberField.class).id("stock-count").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertTrue($(form, NumberField.class).id("stock-count").getDescription()
                .contains("300"));

        assertEquals(Availability.AVAILABLE, $(form, AvailabilitySelector.class)
                .id("availability").getValue());
        assertTrue($(form, AvailabilitySelector.class).id("availability")
                .getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertTrue($(form, AvailabilitySelector.class).id("availability")
                .getDescription().contains(book.getAvailability().toString()));

        assertEquals(book.getCategory(),
                $(form, CheckBoxGroup.class).id("category").getValue());
        assertFalse($(form, CheckBoxGroup.class).id("category").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertFalse($(form, TextField.class).id("price").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));

        // WHEN: Modifying the draft to be valid and clicking save
        // button
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.DISCONTINUED);

        test($(form, Button.class).id("save-button")).click();

        // THEN: A notification is shown and the form is closed and the
        // book is saved and appears in the grid on the last row
        assertNotificationForUpdatedBook();

        assertFalse(form.isShown());

        book = test(grid).item(test(grid).size() - 1);
        assertEquals("Modified book", book.getProductName());

        // Clean up the book created for the test case to avoid
        // conflicts
        var id = book.getId();
        ui.getProductService().deleteProduct(id);
    }

    @Test
    @SuppressWarnings("java:S5961")
    public void closing_session_with_unsaved_changes_will_save_draft_and_after_relogin_confirm_dialog_is_shown_and_if_original_product_was_deleted_message_is_shown_and_draft_is_merged_on_empty_form_and_can_be_saved()
            throws ServiceException {
        // GIVEN: A book is created
        createBook("Draft book");

        // WHEN: Searching for the book
        test($(FilterField.class).id("filter-field")).setValue("Draft book");

        // THEN: The book is found on the first row of the grid and the
        // form is
        // shown
        test(grid).click(1, 0);
        var book = test(grid).item(0);
        assertTrue(form.isShown());

        // WHEN: Book is modified and session is closed
        test($(form, TextField.class).id("product-name"))
                .setValue("Modified book");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);

        // This will close the ui by force, same as closing browser
        tearDown();

        // THEN: A draft is saved
        assertNotNull(service.findDraft(user));

        // WHEN: Simulating other user deleting the book and starting
        // the UI
        // again
        service.deleteProduct(book.getId());

        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        // THEN: A dialog is shown to continue with the draft
        var dialog = $(Window.class).id("confirm-dialog");

        // WHEN: Confirming to edit draft
        test($(dialog, Button.class).id("confirm-button")).click();

        // THEN: Books view is shown with the form open
        grid = $(BookGrid.class).single();
        waitForGrid((CssLayout) grid.getParent(), grid);
        form = $(BookForm.class).single();
        assertTrue(form.isShown());

        // THEN: Draft is removed from the database
        assertNull(service.findDraft(user));

        // THEN: Message is shown that the original product was deleted
        assertNotNull($(Notification.class).stream()
                .filter(n -> n.getCaption().equals("Product was deleted."))
                .findFirst().orElse(null));

        // THEN: The form is populated with the draft data and the form
        // is marked as dirty
        assertEquals("Modified book",
                $(form, TextField.class).id("product-name").getValue());
        assertTrue($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));

        assertEquals(Integer.valueOf(0),
                $(form, NumberField.class).id("stock-count").getValue());
        assertFalse($(form, NumberField.class).id("stock-count").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));

        assertEquals(Availability.AVAILABLE, $(form, AvailabilitySelector.class)
                .id("availability").getValue());
        assertTrue($(form, AvailabilitySelector.class).id("availability")
                .getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertTrue($(form, AvailabilitySelector.class).id("availability")
                .getDescription().contains(Availability.COMING.toString()));

        assertEquals(book.getCategory(),
                $(form, CheckBoxGroup.class).id("category").getValue());
        assertTrue($(form, CheckBoxGroup.class).id("category").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertTrue($(form, CheckBoxGroup.class).id("category").getDescription()
                .contains("[]"));
        assertTrue($(form, TextField.class).id("price").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));

        // WHEN: Modifying the draft to be valid and clicking save
        // button
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.DISCONTINUED);
        test($(form, Button.class).id("save-button")).click();

        assertNotificationForUpdatedBook();

        assertFalse(form.isShown());

        book = test(grid).item(test(grid).size() - 1);
        assertEquals("Modified book", book.getProductName());

        // Clean up the book created for the test case to avoid
        // conflicts
        var id = book.getId();
        ui.getProductService().deleteProduct(id);
    }

    @Test
    public void closing_session_with_unsaved_changes_will_save_draft_and_confirm_dialog_is_shown_on_relogin_and_canceling_it_will_delete_draft()
            throws ServiceException {
        // WHEN: Clicking the first book in the grid
        test(grid).click(1, 0);

        // THEN: The form is shown
        assertTrue(form.isShown());

        // WHEN: Modifying the book and closing the session
        test($(form, TextField.class).id("product-name"))
                .setValue("Modified book");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);

        // This will close the ui by force, same as closing browser
        tearDown();

        // THEN: A draft is saved
        assertNotNull(service.findDraft(user));

        // WHEN: Starting the UI again and logging in
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        // THEN: A dialog is shown to continue with the draft
        var dialog = $(Window.class).id("confirm-dialog");

        // WHEN: Canceling the dialog
        test($(dialog, Button.class).id("cancel-button")).click();

        // THEN: The draft is removed and the about (default) view is
        // shown
        assertNull(service.findDraft(user));

        var about = $(AboutView.class).single();
        assertNotNull(about);
    }

    @SuppressWarnings("unchecked")
    private void createBook(String name) {
        test($(view, Button.class).id("new-product")).click();
        test($(TextField.class).id("product-name")).setValue(name);
        test($(AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.COMING);
        test($(TextField.class).id("price")).setValue("35.0 €");
        var categories = VaadinCreateUI.get().getProductService()
                .getAllCategories().stream().toList();
        test($(CheckBoxGroup.class).id("category"))
                .clickItem(categories.get(1));
        test($(CheckBoxGroup.class).id("category"))
                .clickItem(categories.get(2));
        test($(NumberField.class).id("stock-count")).setValue(0);
        test($(Button.class).id("save-button")).click();
    }
}
