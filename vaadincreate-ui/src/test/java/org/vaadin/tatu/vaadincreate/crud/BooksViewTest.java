package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AboutView;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.locking.LockedObjects;

import com.vaadin.data.ValueContext;
import com.vaadin.server.ServiceException;
import com.vaadin.ui.Button;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class BooksViewTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private BooksView view;
    private BookGrid grid;
    private BookForm form;
    private Collection<Product> backup;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        backup = ui.getProductService().backup();

        view = navigate(BooksView.VIEW_NAME, BooksView.class);

        var layout = $(view, VerticalLayout.class).first();
        grid = $(layout, BookGrid.class).single();
        waitForGrid(layout, grid);
        form = $(view, BookForm.class).single();
    }

    @After
    public void cleanUp() {
        ui.getProductService().restore(backup);
        logout();
        tearDown();
    }

    @Test
    public void selectProduct() throws ServiceException {
        for (int i = 0; i < test(grid).size(); i += 10) {

            test(grid).click(1, i);

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

            test(grid).click(1, i);

            assertEquals("", form.productName.getValue());
            assertEquals("0", form.stockCount.getValue());
            assertEquals("0.00 €", form.price.getValue());
            assertEquals(Availability.COMING, form.availability.getValue());
            assertEquals(Collections.emptySet(), form.category.getValue());
        }

    }

    @Test
    public void crossValidationAndDiscard() {
        test($(view, Button.class).id("new-product")).click();

        test(form.productName).setValue("Te");
        test(form.availability).clickItem(Availability.COMING);
        test(form.price).setValue("10.0 €");
        test(form.stockCount).setValue("10");
        var cat = ui.getProductService().getAllCategories().stream().findFirst()
                .get();
        test(form.category).clickItem(cat);

        test(form.save).click();
        assertFalse(form.save.isEnabled());
        assertTrue(test(form.availability).isInvalid());
        assertTrue(test(form.stockCount).isInvalid());

        test(form.stockCount).setValue("0");
        assertFalse(test(form.availability).isInvalid());
        assertFalse(test(form.stockCount).isInvalid());
        assertTrue(form.save.isEnabled());

        test($(form, Button.class).caption("Cancel").single()).click();

        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("cancel-button")).click();

        assertTrue(form.isShown());

        test($(form, Button.class).caption("Discard").single()).click();
        assertEquals("", form.productName.getValue());
        assertEquals("0.00 €", form.price.getValue());
        assertTrue(form.category.getValue().isEmpty());

        test($(form, Button.class).caption("Cancel").single()).click();
        assertFalse(form.isShown());
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

        // New book is added to the end
        int row = test(grid).size() - 1;
        assertEquals("New book", test(grid).cell(1, row));
        assertEquals("10.00 €", test(grid).cell(2, row));
        assertEquals("10", test(grid).cell(4, row));

        // Find by filter and its the first row
        test($(TextField.class).id("filter-field")).setValue("New book");
        assertEquals(1, test(grid).size());
        assertEquals("New book", test(grid).cell(1, 0));
        assertEquals("10.00 €", test(grid).cell(2, 0));
        assertEquals("10", test(grid).cell(4, 0));
    }

    @Test
    public void addAndCancel() {
        test($(view, Button.class).id("new-product")).click();
        assertTrue(form.isShown());

        test(form.productName).setValue("New book");
        test(form.price).setValue("10.0 €");
        test(form.availability).clickItem(Availability.AVAILABLE);
        test(form.stockCount).setValue("10");

        test($(form, Button.class).caption("Cancel").single()).click();

        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        assertFalse(form.isShown());
    }

    @Test
    public void addAndCancelEmpty() {
        test($(view, Button.class).id("new-product")).click();
        assertTrue(form.isShown());

        test($(form, Button.class).caption("Cancel").single()).click();
        assertFalse(form.isShown());
    }

    @Test
    public void deleteProduct() {
        var book = test(grid).item(0);
        test(grid).click(1, 0);

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
        test(grid).click(1, 0);
        assertTrue(form.isShown());

        var id = book.getId();

        test(form.productName).setValue("Edited book");
        test(form.save).click();
        assertFalse(form.isShown());

        var edited = ui.getProductService().getProductById(id);

        var name = (String) test(grid).cell(1, 0);

        assertEquals("Edited book", name);
        assertEquals("Edited book", edited.getProductName());
        assertEquals(VaadinCreateTheme.BOOKVIEW_GRID_EDITED,
                test(grid).styleName(0));

    }

    @Test
    public void editAndCancel() {
        var book = test(grid).item(0);
        test(grid).click(1, 0);
        assertTrue(form.isShown());

        test(form.productName).setValue("Edited book");

        test($(form, Button.class).caption("Cancel").single()).click();
        assertTrue(form.productName.getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertTrue(LockedObjects.get().isLocked(Product.class,
                book.getId()) != null);

        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        assertFalse(form.isShown());
        assertFalse(LockedObjects.get().isLocked(Product.class,
                book.getId()) != null);
    }

    @Test
    public void openProductChangedByOtherUser() {
        var book = test(grid).item(0);

        // Simulate other user persisting a change to the database
        var edited = ui.getProductService().getProductById(book.getId());
        edited.setProductName("Touched book");
        ui.getProductService().updateProduct(edited);

        test(grid).click(1, 0);
        assertTrue(form.isShown());

        // Assert that change is visible when product is opened
        assertEquals("Touched book", form.productName.getValue());

        var name = (String) test(grid).cell(1, 0);
        assertEquals("Touched book", name);
    }

    @Test
    public void editLockedProduct() {
        var book = test(grid).item(0);
        LockedObjects.get().lock(Product.class, book.getId(),
                CurrentUser.get().get());

        assertEquals("Edited by Admin", test(grid).description(0));
        test(grid).click(1, 0);
        assertFalse(form.isShown());
        LockedObjects.get().unlock(Product.class, book.getId());
    }

    @Test
    public void lockBookUnlockBook() {
        var book = test(grid).item(0);
        assertTrue(LockedObjects.get().isLocked(Product.class,
                book.getId()) == null);

        test(grid).click(1, 0);
        assertTrue(form.isShown());
        assertTrue(LockedObjects.get().isLocked(Product.class,
                book.getId()) != null);

        test(grid).click(1, 1);
        assertTrue(form.isShown());
        assertTrue(LockedObjects.get().isLocked(Product.class,
                book.getId()) == null);
        assertTrue(LockedObjects.get().isLocked(Product.class,
                test(grid).item(1).getId()) != null);

        test(grid).click(1, 1);
        assertFalse(form.isShown());
        assertTrue(LockedObjects.get().isLocked(Product.class,
                test(grid).item(1).getId()) == null);
    }

    @Test
    public void lockBookUnlockOnNavigate() {
        var book = test(grid).item(0);
        assertTrue(LockedObjects.get().isLocked(Product.class,
                book.getId()) == null);

        test(grid).click(1, 0);
        assertTrue(form.isShown());
        assertTrue(LockedObjects.get().isLocked(Product.class,
                book.getId()) != null);

        $(Button.class).caption("About").single().click();
        assertTrue(LockedObjects.get().isLocked(Product.class,
                book.getId()) == null);
    }

    @Test
    public void editProductDiscardChanges() {
        test(grid).click(1, 0);

        test(form.productName).setValue("Edited book");
        test(form.stockCount).setValue("100");

        test(grid).click(1, 1);

        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        assertFalse(form.isShown());
        assertFalse(test(grid).styleName(0)
                .contains(VaadinCreateTheme.BOOKVIEW_GRID_LOCKED));
    }

    @Test
    public void editProductDiscardChangesWhenNavigate() {
        test(grid).click(1, 0);

        test(form.productName).setValue("Edited book");
        test(form.stockCount).setValue("100");

        $(Button.class).caption("About").single().click();

        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        assertTrue($(AboutView.class).size() == 1);
        assertTrue($(BooksView.class).size() == 0);
    }

    @Test
    public void editProductDiscardChangesWhenLogout() {
        test(grid).click(1, 0);

        test(form.productName).setValue("Edited book");
        test(form.stockCount).setValue("100");

        logout();

        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("cancel-button")).click();

        assertTrue($(BooksView.class).size() == 1);
        assertTrue(form.isShown());
    }

    @Test
    public void validationError() {
        test(grid).click(1, 0);

        test(form.productName).setValue("");
        test(form.stockCount).focus();
        assertTrue(test(form.productName).isInvalid());
    }

    @Test
    public void resizeTest() {
        ui.getPage().updateBrowserWindowSize(1600, 1024, true);

        grid.getColumns().forEach(col -> {
            assertFalse(col.isHidden());
        });

        ui.getPage().updateBrowserWindowSize(1200, 1024, true);

        assertTrue(grid.getColumns().get(0).isHidden());
        assertFalse(grid.getColumns().get(1).isHidden());
        assertFalse(grid.getColumns().get(2).isHidden());
        assertFalse(grid.getColumns().get(3).isHidden());
        assertFalse(grid.getColumns().get(4).isHidden());
        assertTrue(grid.getColumns().get(5).isHidden());

        ui.getPage().updateBrowserWindowSize(900, 1024, true);

        assertTrue(grid.getColumns().get(0).isHidden());
        assertFalse(grid.getColumns().get(1).isHidden());
        assertFalse(grid.getColumns().get(2).isHidden());
        assertFalse(grid.getColumns().get(3).isHidden());
        assertTrue(grid.getColumns().get(4).isHidden());
        assertTrue(grid.getColumns().get(5).isHidden());

        ui.getPage().updateBrowserWindowSize(500, 1024, true);

        assertTrue(grid.getColumns().get(0).isHidden());
        assertFalse(grid.getColumns().get(1).isHidden());
        assertFalse(grid.getColumns().get(2).isHidden());
        assertTrue(grid.getColumns().get(3).isHidden());
        assertTrue(grid.getColumns().get(4).isHidden());
        assertTrue(grid.getColumns().get(5).isHidden());
    }

    @Test
    public void sortingByPrice() {
        int size = test(grid).size();

        test(grid).toggleColumnSorting(2);

        for (int i = 1; i < size; i++) {
            var result = test(grid).item(i - 1).getPrice()
                    .compareTo(test(grid).item(i).getPrice());
            assertTrue(result <= 0);
        }

        test(grid).toggleColumnSorting(2);

        for (int i = 1; i < size; i++) {
            var result = test(grid).item(i - 1).getPrice()
                    .compareTo(test(grid).item(i).getPrice());
            assertTrue(result >= 0);
        }
    }

    @Test
    public void sortingByName() {
        int size = test(grid).size();

        test(grid).toggleColumnSorting(1);

        for (int i = 1; i < size; i++) {
            var result = ((String) test(grid).cell(1, i - 1))
                    .compareToIgnoreCase((String) test(grid).cell(1, i));
            assertTrue(result <= 0);
        }

        test(grid).toggleColumnSorting(1);

        for (int i = 1; i < size; i++) {
            var result = ((String) test(grid).cell(1, i - 1))
                    .compareToIgnoreCase((String) test(grid).cell(1, i));
            assertTrue(result >= 0);
        }
    }
}
