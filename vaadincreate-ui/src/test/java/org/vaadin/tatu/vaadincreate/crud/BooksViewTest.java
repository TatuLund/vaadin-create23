package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;

import com.vaadin.data.ValueContext;
import com.vaadin.server.ServiceException;
import com.vaadin.ui.Button;
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

        view = (BooksView) navigate(BooksView.VIEW_NAME);

        var layout = (VerticalLayout) $(view, VerticalLayout.class);
        grid = $(layout, BookGrid.class);
        waitForGrid(layout, grid);
        form = $(view, BookForm.class);

    }

    @Test
    public void selectProduct() throws ServiceException {
        for (int i = 0; i < getGridSize(grid); i += 10) {
            var book = getGridItem(grid, i);
            grid.select(book);

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
        }

    }

    @Test
    public void addProduct() throws ServiceException {
        var newButton = (Button) $(view, "new-product");
        newButton.click();

        form.productName.setValue("New book");
        form.price.setValue("10.0 €");
        form.availability.setValue(Availability.AVAILABLE);
        form.stockCount.setValue("10");

        var cat = ProductDataService.get().getAllCategories().stream()
                .findFirst().get();
        form.category.select(cat);

        form.save.click();

        assertTrue(ProductDataService.get().getAllProducts().stream()
                .anyMatch(b -> b.getProductName().equals("New book")));

        int row = getGridSize(grid)-1;
        assertEquals("New book", getGridCell(grid, 1, row));
        assertEquals("10.00 €", getGridCell(grid, 2, row));
        assertEquals("10", getGridCell(grid, 4, row));
        assertEquals(cat.getName(), getGridCell(grid, 5, row));
    }

    @Test
    public void deleteProduct() {
        var book = getGridItem(grid, 0);
        grid.select(book);

        var id = book.getId();
        var name = book.getProductName();

        form.delete.click();

        var dialog = (Window) ui.getWindows().stream().findFirst().get();
        var confirm = (Button) $(dialog, "confirm-button");
        confirm.click();

        assertEquals(null, ProductDataService.get().getProductById(id));

        var newName = getGridCell(grid, 1, 0);
        assertNotEquals(name, newName);
    }

    @Test
    public void editProduct() {
        var book = getGridItem(grid, 0);
        grid.select(book);

        var id = book.getId();

        form.productName.setValue("Edited book");
        form.save.click();

        var edited = ProductDataService.get().getProductById(id);

        var name = (String) getGridCell(grid, 1, 0);

        assertEquals("Edited book", name);
        assertEquals("Edited book", edited.getProductName());
    }

    private void waitForGrid(VerticalLayout layout, BookGrid grid) {
        assertFalse(grid.isVisible());

        var fake = $(layout, FakeGrid.class);
        waitWhile(fake, f -> f.isVisible());
        assertTrue(grid.isVisible());
    }

}