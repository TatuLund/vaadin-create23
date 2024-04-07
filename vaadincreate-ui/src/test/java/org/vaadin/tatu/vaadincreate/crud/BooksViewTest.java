package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;

import com.vaadin.data.ValueContext;
import com.vaadin.server.ServiceException;
import com.vaadin.ui.Button;
import com.vaadin.ui.Notification;
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
    public void selectProduct() throws ServiceException {
        for (int i = 0; i < test(grid).size(); i += 10) {

            test(grid).click(0, i);

            var book = test(grid).item(i);
            assertEquals(book.getProductName(), form.productName.getValue());
            var price = form.price;
            var converter = new EuroConverter("");
            assertEquals(
                    converter.convertToPresentation(book.getPrice(),
                            new ValueContext(null, price, ui.getLocale())),
                    price.getValue());
            assertEquals("" + book.getStockCount(), form.stockCount.getValue());

            assertEquals(book.getAvailability(), form.availability.getValue());
            assertEquals(book.getCategory(), form.category.getValue());

            test(grid).click(0, i);

            assertEquals("", form.productName.getValue());
            assertEquals("0", form.stockCount.getValue());
            assertEquals("0.00 €", form.price.getValue());
            assertEquals(Availability.COMING, form.availability.getValue());
            assertEquals(Collections.emptySet(), form.category.getValue());
        }

    }

    @Test
    public void addProduct() {
        test($(view, Button.class).id("new-product")).click();

        test(form.productName).setValue("New book");
        test(form.price).setValue("10.0 €");
        test(form.availability).clickItem(Availability.AVAILABLE);
        test(form.stockCount).setValue("10");

        var cat = ui.getProductService().getAllCategories().stream().findFirst()
                .get();
        test(form.category).clickItem(cat);

        test(form.save).click();

        assertTrue(
                $(Notification.class).last().getCaption().contains("New book"));

        assertTrue(ui.getProductService().getAllProducts().stream()
                .anyMatch(b -> b.getProductName().equals("New book")));

        int row = test(grid).size() - 1;
        assertEquals("New book", test(grid).cell(1, row));
        assertEquals("10.00 €", test(grid).cell(2, row));
        assertEquals("10", test(grid).cell(4, row));
        assertEquals(cat.getName(), test(grid).cell(5, row));
    }

    @Test
    public void deleteProduct() {
        var book = test(grid).item(0);
        test(grid).click(0, 0);

        var id = book.getId();
        var name = book.getProductName();

        test(form.delete).click();

        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        assertTrue($(Notification.class).last().getCaption()
                .contains(book.getProductName()));

        assertEquals(null, ui.getProductService().getProductById(id));

        var newName = test(grid).cell(1, 0);
        assertNotEquals(name, newName);
    }

    @Test
    public void editProduct() {
        var book = test(grid).item(0);
        test(grid).click(0, 0);

        var id = book.getId();

        test(form.productName).setValue("Edited book");
        test(form.save).click();

        var edited = ui.getProductService().getProductById(id);

        var name = (String) test(grid).cell(1, 0);

        assertEquals("Edited book", name);
        assertEquals("Edited book", edited.getProductName());
    }

    @Test
    public void editProductDiscardChanges() {
        test(grid).click(0, 0);

        test(form.productName).setValue("Edited book");
        test(form.stockCount).setValue("100");

        test(grid).click(0, 1);

        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        assertFalse(form.isShown());
    }

    @Test
    public void validationError() {
        test(grid).click(0, 0);

        test(form.productName).setValue("");
        test(form.stockCount).focus();
        assertTrue(test(form.productName).isInvalid());
    }

    private void waitForGrid(VerticalLayout layout, BookGrid grid) {
        assertFalse(grid.isVisible());

        var fake = $(layout, FakeGrid.class).first();
        waitWhile(fake, f -> f.isVisible(), 10);
        assertTrue(grid.isVisible());
    }

}
