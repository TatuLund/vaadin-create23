package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.locking.LockedObjects;

import com.vaadin.data.ValueContext;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinSession;
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
            assertTrue(test(grid).isFocused());

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

            verifySelectedCategoriesAreTheFirst();

            test(grid).click(1, i);

            assertEquals("", form.productName.getValue());
            assertEquals("0", form.stockCount.getValue());
            assertEquals("0.00 €", form.price.getValue());
            assertEquals(Availability.COMING, form.availability.getValue());
            assertEquals(Collections.emptySet(), form.category.getValue());
        }

    }

    private void verifySelectedCategoriesAreTheFirst() {
        // Verify that the selected categories are the first in the list
        var items = form.category.getDataCommunicator().fetchItemsWithRange(0,
                form.category.getDataCommunicator().getDataProviderSize());
        var size = form.category.getValue().size();
        for (int j = 0; j < size; j++) {
            assertTrue(form.category.getValue().contains(items.get(j)));
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

        test(form.saveButton).click();
        assertFalse(form.saveButton.isEnabled());
        assertTrue(test(form.availability).isInvalid());
        var errorMessage = "Mismatch between availability and stock count";
        assertEquals(errorMessage, test(form.availability).errorMessage());
        assertTrue(test(form.stockCount).isInvalid());
        assertEquals(errorMessage, test(form.stockCount).errorMessage());

        test(form.stockCount).setValue("0");
        assertFalse(test(form.availability).isInvalid());
        assertFalse(test(form.stockCount).isInvalid());
        assertTrue(form.saveButton.isEnabled());

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
        assertTrue(test(grid).isFocused());
    }

    @Test
    public void addProduct() {
        test($(view, Button.class).id("new-product")).click();

        assertTrue(test(form.productName).isFocused());
        test(form.productName).setValue("New book");
        test(form.price).setValue("10.0 €");
        test(form.availability).clickItem(Availability.AVAILABLE);
        test(form.stockCount).setValue("10");

        var cat = ui.getProductService().getAllCategories().stream().findFirst()
                .get();
        test(form.category).clickItem(cat);
        verifySelectedCategoriesAreTheFirst();

        test(form.saveButton).click();

        assertTrue(test(grid).isFocused());

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
        assertTrue(test(grid).isFocused());
    }

    @Test
    public void addAndCancelEmpty() {
        test($(view, Button.class).id("new-product")).click();
        assertTrue(form.isShown());

        test($(form, Button.class).caption("Cancel").single()).click();
        assertFalse(form.isShown());
        assertTrue(test(grid).isFocused());
    }

    @Test
    public void deleteProduct() {
        var book = test(grid).item(0);
        test(grid).click(1, 0);

        var id = book.getId();
        var name = book.getProductName();

        test(form.deleteButton).click();

        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        assertTrue($(Notification.class).last().getCaption()
                .contains(book.getProductName()));
        assertFalse(form.isShown());

        assertEquals(null, ui.getProductService().getProductById(id));

        var newName = test(grid).cell(1, 0);
        assertNotEquals(name, newName);
    }

    @Test
    public void concurrentDelete() {
        // Simulate other user deleting the book
        var book = test(grid).item(0);
        var name = book.getProductName();
        ui.getProductService().deleteProduct(book.getId());

        test(grid).click(1, 0);
        assertEquals("Product was deleted.",
                $(Notification.class).last().getCaption());
        assertFalse(form.isShown());

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
        test(form.saveButton).click();
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
        assertNotNull(LockedObjects.get().isLocked(book));

        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        assertFalse(form.isShown());
        assertTrue(test(grid).isFocused());

        assertNull(LockedObjects.get().isLocked(book));
    }

    @Test
    public void editAndSelectAndConfirmCancel() {
        test(grid).click(1, 0);
        assertTrue(form.isShown());

        test(form.productName).setValue("Changed book");

        test(grid).click(1, 1);
        assertTrue(form.productName.getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();
        assertFalse(form.isShown());

        test(grid).click(1, 0);
        assertTrue(form.isShown());
        assertFalse(form.productName.getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
    }

    @Test
    public void editAndSelectAndCancelCancel() {
        var book = test(grid).item(0);
        test(grid).click(1, 0);
        assertTrue(form.isShown());

        test(form.productName).setValue("Changed book");

        test(grid).click(1, 1);
        assertTrue(form.productName.getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("cancel-button")).click();

        assertTrue(form.isShown());
        assertTrue(form.productName.getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));

        assertEquals(book, grid.getSelectedRow());

        test(form.discardButton).click();
        assertFalse(form.productName.getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
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
        LockedObjects.get().lock(book, CurrentUser.get().get());

        assertEquals("Edited by Admin", test(grid).description(0));
        test(grid).click(1, 0);
        assertFalse(form.isShown());
        LockedObjects.get().unlock(book);
    }

    @Test
    public void weakLockConcurrentEdit() {
        var book = test(grid).item(0);
        LockedObjects.get().lock(book, CurrentUser.get().get());
        assertEquals("Edited by Admin", test(grid).description(0));

        // GC wipes weak lock
        System.gc();

        test(grid).click(1, 0);
        assertTrue(form.isShown());

        // Concurrent edit is now possible, simulate it
        ui.getProductService().updateProduct(book);

        // Change and save
        test(form.productName).setValue("Product name changed");
        test(form.saveButton).click();

        // Assert internal error happens
        assertEquals("Internal error.",
                $(Notification.class).last().getCaption());
    }

    @Test
    public void weakLockConcurrentDelete() {
        var book = test(grid).item(0);
        LockedObjects.get().lock(book, CurrentUser.get().get());
        assertEquals("Edited by Admin", test(grid).description(0));

        // GC wipes weak lock
        System.gc();

        test(grid).click(1, 0);
        assertTrue(form.isShown());

        // Concurrent delete is now possible, simulate it
        ui.getProductService().deleteProduct(book.getId());

        // Change and save
        test(form.productName).setValue("Product name changed");
        test(form.saveButton).click();

        // Assert internal error happens
        assertEquals("Internal error.",
                $(Notification.class).last().getCaption());
    }

    @Test
    public void lockBookUnlockBook() {
        var book = test(grid).item(0);
        assertNull(LockedObjects.get().isLocked(book));

        test(grid).click(1, 0);
        assertTrue(form.isShown());
        assertNotNull(LockedObjects.get().isLocked(book));

        test(grid).click(1, 1);
        assertTrue(form.isShown());
        assertNull(LockedObjects.get().isLocked(book));
        assertNotNull(LockedObjects.get().isLocked(test(grid).item(1)));

        test(grid).click(1, 1);
        assertFalse(form.isShown());
        assertNull(LockedObjects.get().isLocked(test(grid).item(1)));
    }

    @Test
    public void lockBookUnlockOnNavigate() {
        var book = test(grid).item(0);
        assertNull(LockedObjects.get().isLocked(book));

        test(grid).click(1, 0);
        assertTrue(form.isShown());
        assertNotNull(LockedObjects.get().isLocked(book));

        $(Button.class).caption("About").single().click();
        assertNull(LockedObjects.get().isLocked(book));
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

        assertEquals(1, $(AboutView.class).size());
        assertEquals(0, $(BooksView.class).size());
    }

    @Test
    public void editProductDiscardChangesWhenLogout() {
        test(grid).click(1, 0);

        test(form.productName).setValue("Edited book");
        test(form.stockCount).setValue("100");

        logout();

        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("cancel-button")).click();

        assertEquals(1, $(BooksView.class).size());
        assertTrue(form.isShown());
    }

    @Test
    public void validationError() {
        test(grid).click(1, 0);

        test(form.productName).setValue("");
        test(form.stockCount).focus();
        assertTrue(test(form.productName).isInvalid());
        assertEquals(
                "Product name must have at least two characters and maximum of 100",
                test(form.productName).errorMessage());
    }

    @Test
    public void categoriesValid() {
        var category = new Category();
        category.setName("Science");
        category = ui.getProductService().updateCategory(category);

        test(grid).click(1, 0);

        // Simulate other user deleting the category while editor is open
        ui.getProductService().deleteCategory(category.getId());

        test(form.category).clickItem(category);
        test(form.saveButton).click();

        assertEquals("One or more of the selected categories were deleted.",
                $(Notification.class).last().getCaption());
    }

    @Test
    public void resizeTest() {
        ui.getPage().updateBrowserWindowSize(1600, 1024, true);

        grid.getColumns().forEach(col -> {
            assertFalse(col.isHidden());
        });

        assertEquals(null, test(grid).description(0));

        ui.getPage().updateBrowserWindowSize(1200, 1024, true);

        assertTrue(grid.getColumns().get(0).isHidden());
        assertFalse(grid.getColumns().get(1).isHidden());
        assertFalse(grid.getColumns().get(2).isHidden());
        assertFalse(grid.getColumns().get(3).isHidden());
        assertFalse(grid.getColumns().get(4).isHidden());
        assertTrue(grid.getColumns().get(5).isHidden());

        assertEquals(null, test(grid).description(0));

        ui.getPage().updateBrowserWindowSize(900, 1024, true);

        assertTrue(grid.getColumns().get(0).isHidden());
        assertFalse(grid.getColumns().get(1).isHidden());
        assertFalse(grid.getColumns().get(2).isHidden());
        assertFalse(grid.getColumns().get(3).isHidden());
        assertTrue(grid.getColumns().get(4).isHidden());
        assertTrue(grid.getColumns().get(5).isHidden());

        assertEquals(null, test(grid).description(0));

        ui.getPage().updateBrowserWindowSize(500, 1024, true);

        assertTrue(grid.getColumns().get(0).isHidden());
        assertFalse(grid.getColumns().get(1).isHidden());
        assertFalse(grid.getColumns().get(2).isHidden());
        assertTrue(grid.getColumns().get(3).isHidden());
        assertTrue(grid.getColumns().get(4).isHidden());
        assertTrue(grid.getColumns().get(5).isHidden());

        assertTrue(test(grid).description(0)
                .contains(test(grid).item(0).getProductName()));
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

    @Test
    public void isSerializable() throws IOException, ClassNotFoundException {
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
