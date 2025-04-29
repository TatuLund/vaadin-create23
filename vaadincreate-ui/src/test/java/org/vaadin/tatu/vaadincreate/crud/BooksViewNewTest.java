package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.crud.form.AvailabilitySelector;
import org.vaadin.tatu.vaadincreate.crud.form.BookForm;
import org.vaadin.tatu.vaadincreate.crud.form.NumberField;

import com.vaadin.server.ServiceException;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class BooksViewNewTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private BooksView view;
    private BookGrid grid;
    private BookForm form;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        view = navigate(BooksView.VIEW_NAME + "/new", BooksView.class);

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
    @SuppressWarnings("unchecked")
    public void when_books_view_is_entered_with_new_as_url_parameter_form_is_shown_for_new_product_which_can_be_entered_and_saved() {
        // THEN: The form is shown and the first field is focused
        assertTrue(form.isShown());
        assertTrue(
                test($(form, TextField.class).id("product-name")).isFocused());

        // WHEN: The form is filled and save button is clicked
        test($(form, TextField.class).id("product-name"))
                .setValue("A new product");
        test($(form, TextField.class).id("price")).setValue("10.0 €");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);
        test($(form, NumberField.class).id("stock-count")).setValue(10);

        var cat = ui.getProductService().getAllCategories().stream().findFirst()
                .get();
        test($(form, CheckBoxGroup.class).id("category")).clickItem(cat);

        test($(form, Button.class).id("save-button")).click();

        // THEN: The form is hidden, a notification is shown, the grid is
        // focused
        assertNotificationForUpdatedBook();
        assertFalse(form.isShown());

        assertTrue(test(grid).isFocused());

        // THEN: The product is in the database
        assertTrue(ui.getProductService().getAllProducts().stream()
                .anyMatch(b -> b.getProductName().equals("A new product")));

        // THEN: The product is in the grid as the last row and the data is
        // correct
        int row = test(grid).size() - 1;
        assertEquals("A new product", test(grid).cell(1, row));
        assertEquals("10.00 €", test(grid).cell(2, row));
        assertEquals("10", test(grid).cell(4, row));

        // Clean up
        ui.getProductService().deleteProduct(test(grid).item(row).getId());
    }

    private void assertNotificationForUpdatedBook() {
        assertTrue($(Notification.class).stream()
                .filter(n -> n.getCaption().equals("\"A new product\" updated"))
                .findAny().isPresent());
    }
}
