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
import java.util.stream.Collectors;

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
    public void selectProduct() {
        for (int i = 0; i < test(grid).size(); i += 10) {

            test(grid).click(1, i);
            assertTrue(test(grid).isFocused());

            var book = test(grid).item(i);
            assertEquals(book.getProductName(),
                    $(form, TextField.class).id("product-name").getValue());
            var price = $(form, TextField.class).id("price");
            var converter = new EuroConverter("");
            assertEquals(
                    converter.convertToPresentation(book.getPrice(),
                            new ValueContext(null, price, ui.getLocale())),
                    price.getValue());
            assertEquals(Integer.valueOf(book.getStockCount()),
                    $(form, NumberField.class).id("stock-count").getValue());

            assertEquals(book.getAvailability(),
                    $(form, AvailabilitySelector.class).id("availability")
                            .getValue());
            assertEquals(book.getCategory(),
                    $(form, CheckBoxGroup.class).id("category").getValue());

            verifySelectedCategoriesAreTheFirst();

            test(grid).click(1, i);

            assertEquals("",
                    $(form, TextField.class).id("product-name").getValue());
            assertEquals(Integer.valueOf(0),
                    $(form, NumberField.class).id("stock-count").getValue());
            assertEquals("0.00 €",
                    $(form, TextField.class).id("price").getValue());
            assertEquals(Availability.COMING,
                    $(form, AvailabilitySelector.class).id("availability")
                            .getValue());
            assertEquals(Collections.emptySet(),
                    $(form, CheckBoxGroup.class).id("category").getValue());
        }

    }

    private void verifySelectedCategoriesAreTheFirst() {
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
    @SuppressWarnings("unchecked")
    public void crossValidationAndDiscard() {
        test($(view, Button.class).id("new-product")).click();
        assertFalse($(form, Button.class).id("discard-button").isEnabled());
        assertFalse($(form, Button.class).id("save-button").isEnabled());

        test($(form, TextField.class).id("product-name")).setValue("Te");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.COMING);
        test($(form, TextField.class).id("price")).setValue("10.0 €");
        test($(form, NumberField.class).id("stock-count")).setValue(10);
        var cat = ui.getProductService().getAllCategories().stream().findFirst()
                .get();
        test($(form, CheckBoxGroup.class).id("category")).clickItem(cat);

        test($(form, Button.class).id("save-button")).click();
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

        test($(form, NumberField.class).id("stock-count")).setValue(0);
        assertFalse(test($(form, AvailabilitySelector.class).id("availability"))
                .isInvalid());
        assertFalse(
                test($(form, NumberField.class).id("stock-count")).isInvalid());
        assertTrue($(form, Button.class).id("save-button").isEnabled());

        test($(form, Button.class).caption("Cancel").single()).click();

        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("cancel-button")).click();

        assertTrue(form.isShown());

        test($(form, Button.class).caption("Discard").single()).click();
        assertEquals("",
                $(form, TextField.class).id("product-name").getValue());
        assertEquals("0.00 €", $(form, TextField.class).id("price").getValue());
        assertTrue($(form, CheckBoxGroup.class).id("category").getValue()
                .isEmpty());

        test($(form, Button.class).caption("Cancel").single()).click();
        assertFalse(form.isShown());
        assertTrue(test(grid).isFocused());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void addProduct() {
        test($(view, Button.class).id("new-product")).click();

        assertTrue(
                test($(form, TextField.class).id("product-name")).isFocused());
        test($(form, TextField.class).id("product-name"))
                .setValue("Filter book");
        test($(form, TextField.class).id("price")).setValue("10.0 €");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);
        test($(form, NumberField.class).id("stock-count")).setValue(10);

        var cat = ui.getProductService().getAllCategories().stream().findFirst()
                .get();
        test($(form, CheckBoxGroup.class).id("category")).clickItem(cat);
        verifySelectedCategoriesAreTheFirst();

        test($(form, Button.class).id("save-button")).click();

        assertTrue(test(grid).isFocused());

        assertTrue($(Notification.class).last().getCaption()
                .contains("Filter book"));

        assertTrue(ui.getProductService().getAllProducts().stream()
                .anyMatch(b -> b.getProductName().equals("Filter book")));

        // New book is added to the end
        int row = test(grid).size() - 1;
        assertEquals("Filter book", test(grid).cell(1, row));
        assertEquals("10.00 €", test(grid).cell(2, row));
        assertEquals("10", test(grid).cell(4, row));

        // Find by filter and its the first row
        test($(FilterField.class).id("filter-field")).setValue("Filter book");
        assertEquals(1, test(grid).size());
        assertEquals("Filter book", test(grid).cell(1, 0));
        assertEquals("10.00 €", test(grid).cell(2, 0));
        assertEquals("10", test(grid).cell(4, 0));

        // Cleanup
        ui.getProductService().deleteProduct(test(grid).item(0).getId());
    }

    @Test
    public void addAndCancel() {
        test($(view, Button.class).id("new-product")).click();
        assertTrue(form.isShown());

        test($(form, TextField.class).id("product-name")).setValue("New book");
        test($(form, TextField.class).id("price")).setValue("10.0 €");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);
        test($(form, NumberField.class).id("stock-count")).setValue(10);

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
        createBook("Delete book");

        test($(FilterField.class).id("filter-field")).setValue("Delete book");

        var row = 0;
        var book = test(grid).item(row);
        var id = book.getId();
        assertEquals("Delete book", book.getProductName());
        test(grid).click(1, row);

        test($(form, Button.class).id("delete-button")).click();

        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        assertTrue($(Notification.class).last().getCaption()
                .contains(book.getProductName()));
        assertFalse(form.isShown());

        assertEquals(null, ui.getProductService().getProductById(id));

        assertEquals(0, test(grid).size());
    }

    @SuppressWarnings("unchecked")
    private void createBook(String name) {
        test($(view, Button.class).id("new-product")).click();
        test($(TextField.class).id("product-name")).setValue(name);
        test($(AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);
        test($(TextField.class).id("price")).setValue("35.0 €");
        var categories = VaadinCreateUI.get().getProductService()
                .getAllCategories().stream().collect(Collectors.toList());
        test($(CheckBoxGroup.class).id("category"))
                .clickItem(categories.get(1));
        test($(CheckBoxGroup.class).id("category"))
                .clickItem(categories.get(2));
        test($(NumberField.class).id("stock-count")).setValue(100);
        test($(Button.class).id("save-button")).click();
    }

    @Test
    public void concurrentDelete() {
        createBook("Concurrent delete");

        // Simulate other user deleting the book
        test($(FilterField.class).id("filter-field"))
                .setValue("Concurrent delete");
        var book = test(grid).item(0);
        ui.getProductService().deleteProduct(book.getId());

        test(grid).click(1, 0);
        assertEquals("Product was deleted.",
                $(Notification.class).last().getCaption());
        assertFalse(form.isShown());

        assertEquals(0, test(grid).size());
    }

    @Test
    public void editProduct() {
        createBook("Test book");

        test($(FilterField.class).id("filter-field")).setValue("Test book");

        var row = 0;
        var book = test(grid).item(row);
        test(grid).click(1, row);
        assertTrue(form.isShown());

        var id = book.getId();
        assertNotNull(id);

        test($(form, TextField.class).id("product-name"))
                .setValue("Edited book");
        test($(form, Button.class).id("save-button")).click();
        assertFalse(form.isShown());

        var edited = ui.getProductService().getProductById(id);

        test($(FilterField.class).id("filter-field")).setValue("Edited book");
        var name = (String) test(grid).cell(1, row);

        assertEquals("Edited book", name);
        assertEquals("Edited book", edited.getProductName());
        assertEquals(VaadinCreateTheme.BOOKVIEW_GRID_EDITED,
                test(grid).styleName(0));

        ui.getProductService().deleteProduct(id);
    }

    @Test
    public void editAndCancel() {
        var book = test(grid).item(0);
        test(grid).click(1, 0);
        assertTrue(form.isShown());

        test($(form, TextField.class).id("product-name"))
                .setValue("Edited book");

        test($(form, Button.class).caption("Cancel").single()).click();
        assertTrue($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertTrue($(form, TextField.class).id("product-name").getDescription()
                .contains(book.getProductName()));
        assertNotNull(LockedObjects.get().isLocked(book));

        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        assertFalse(form.isShown());
        assertTrue(test(grid).isFocused());

        assertNull(LockedObjects.get().isLocked(book));
    }

    @Test
    public void editAndSelectAndConfirmCancel() {
        var book = test(grid).item(0);
        test(grid).click(1, 0);
        assertTrue(form.isShown());

        test($(form, TextField.class).id("product-name"))
                .setValue("Changed book");

        test(grid).click(1, 1);
        assertTrue($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertTrue($(form, TextField.class).id("product-name").getDescription()
                .contains(book.getProductName()));
        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();
        assertFalse(form.isShown());

        test(grid).click(1, 0);
        assertTrue(form.isShown());
        assertFalse($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertNull(
                $(form, TextField.class).id("product-name").getDescription());

        // Close form gracefully to avoid side effects
        test($(form, Button.class).id("cancel-button")).click();
        assertFalse(form.isShown());
    }

    @Test
    public void editAndSelectAndCancelCancel() {
        var book = test(grid).item(0);
        test(grid).click(1, 0);
        assertTrue(form.isShown());

        test($(form, TextField.class).id("product-name"))
                .setValue("Changed book");

        test(grid).click(1, 1);
        assertTrue($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertTrue($(form, TextField.class).id("product-name").getDescription()
                .contains(book.getProductName()));
        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("cancel-button")).click();

        assertTrue(form.isShown());
        assertTrue($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertTrue($(form, TextField.class).id("product-name").getDescription()
                .contains(book.getProductName()));

        assertEquals(book, grid.getSelectedRow());

        test($(form, Button.class).id("discard-button")).click();
        assertFalse($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertNull(
                $(form, TextField.class).id("product-name").getDescription());

        test($(form, Button.class).id("cancel-button")).click();
        assertFalse(form.isShown());
    }

    @Test
    public void openProductChangedByOtherUser() {
        var book = test(grid).item(0);

        // Simulate other user persisting a change to the database
        var edited = ui.getProductService().getProductById(book.getId());
        var productName = edited.getProductName();
        edited.setProductName("Touched book");
        var saved = ui.getProductService().updateProduct(edited);

        test(grid).click(1, 0);
        assertTrue(form.isShown());

        // Assert that change is visible when product is opened
        assertEquals("Touched book",
                $(form, TextField.class).id("product-name").getValue());

        var name = (String) test(grid).cell(1, 0);
        assertEquals("Touched book", name);

        test($(form, Button.class).id("cancel-button")).click();
        assertFalse(form.isShown());

        edited.setProductName(productName);
        ui.getProductService().updateProduct(saved);
    }

    @Test
    public void editLockedProduct() {
        var book = test(grid).item(15);
        LockedObjects.get().lock(book, CurrentUser.get().get());

        assertEquals("Edited by Admin", test(grid).description(15));
        test(grid).click(1, 15);
        assertFalse(form.isShown());
        LockedObjects.get().unlock(book);
    }

    @Test
    @SuppressWarnings("java:S1854")
    public void weakLockConcurrentEdit() {
        // Idea of this is to simulate Form being detached due browser
        // crash so
        // that form no longer holds the product reference. Here it is
        // simulated
        // by doing lock via proxy object.
        var book = new Product(test(grid).item(0));
        LockedObjects.get().lock(book, CurrentUser.get().get());
        assertEquals("Edited by Admin", test(grid).description(0));
        book = null;

        // GC wipes weak lock
        System.gc();

        test(grid).click(1, 0);
        assertTrue(form.isShown());

        // Concurrent edit is now possible, simulate it
        ui.getProductService().updateProduct(new Product(test(grid).item(0)));

        // Change and save
        test($(form, TextField.class).id("product-name"))
                .setValue("Product name changed");
        test($(form, Button.class).id("save-button")).click();

        // Assert internal error happens
        assertEquals("Internal error.",
                $(Notification.class).last().getCaption());

        assertFalse(form.isShown());
    }

    @Test
    @SuppressWarnings("java:S1854")
    public void weakLockConcurrentDelete() {
        // Idea of this is to simulate Form being detached due browser
        // crash so
        // that form no longer holds the product reference. Here it is
        // simulated
        // by doing lock via proxy object.
        createBook("Concurrent delete");
        test($(FilterField.class).id("filter-field"))
                .setValue("Concurrent delete");

        var book = new Product(test(grid).item(0));
        LockedObjects.get().lock(book, CurrentUser.get().get());
        assertEquals("Edited by Admin", test(grid).description(0));
        book = null;

        // GC wipes weak lock
        System.gc();

        test(grid).click(1, 0);
        assertTrue(form.isShown());

        // Concurrent delete is now possible, simulate it
        ui.getProductService().deleteProduct(test(grid).item(0).getId());

        // Change and save
        test($(form, TextField.class).id("product-name"))
                .setValue("Product name changed");
        test($(form, Button.class).id("save-button")).click();

        // Assert internal error happens
        assertEquals("Internal error.",
                $(Notification.class).last().getCaption());

        assertFalse(form.isShown());
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

        $(MenuButton.class).caption("About").single().click();
        assertNull(LockedObjects.get().isLocked(book));
    }

    @Test
    public void editProductDiscardChanges() {
        test(grid).click(1, 0);

        test($(form, TextField.class).id("product-name"))
                .setValue("Edited book");
        test($(form, NumberField.class).id("stock-count")).setValue(100);

        test(grid).click(1, 1);

        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        assertFalse(form.isShown());
        assertFalse(test(grid).styleName(0)
                .contains(VaadinCreateTheme.BOOKVIEW_GRID_LOCKED));
    }

    @Test
    public void editProductSaveChanges() {
        test(grid).click(1, 0);

        var name = $(form, TextField.class).id("product-name").getValue();
        test($(form, TextField.class).id("product-name"))
                .setValue("Different book");

        test($(form, Button.class).id("save-button")).click();

        assertEquals("\"Different book\" updated",
                $(Notification.class).last().getCaption());

        assertFalse(form.isShown());
        assertTrue(test(grid).styleName(0)
                .contains(VaadinCreateTheme.BOOKVIEW_GRID_EDITED));

        assertEquals("Different book", test(grid).cell(1, 0));

        test(grid).click(1, 0);
        test($(form, TextField.class).id("product-name")).setValue(name);

        test($(form, Button.class).id("save-button")).click();
        assertEquals(name, test(grid).cell(1, 0));
    }

    @Test
    public void editProductRevertEdit() {
        test(grid).click(1, 0);

        var name = $(form, TextField.class).id("product-name").getValue();
        var count = $(form, NumberField.class).id("stock-count").getValue();
        test($(form, TextField.class).id("product-name"))
                .setValue("Edited book");
        test($(form, NumberField.class).id("stock-count")).setValue(100);

        // Assert that change was detected
        assertTrue($(form, Button.class).id("save-button").isEnabled());
        assertTrue($(form, Button.class).id("discard-button").isEnabled());

        // Revert edits
        test($(form, TextField.class).id("product-name")).setValue(name);
        test($(form, NumberField.class).id("stock-count")).setValue(count);

        // Assert that change was detected
        assertFalse($(form, Button.class).id("save-button").isEnabled());
        assertFalse($(form, Button.class).id("discard-button").isEnabled());

        // Attempt to change item
        test(grid).click(1, 1);

        assertTrue(form.isShown());
        // Assert that form content was updated
        assertEquals($(form, TextField.class).id("product-name").getValue(),
                test(grid).cell(1, 1));

        test($(form, Button.class).id("cancel-button")).click();
        assertFalse(form.isShown());
    }

    @Test
    public void editProductDiscardChangesWhenNavigate() {
        test(grid).click(1, 0);

        test($(form, TextField.class).id("product-name"))
                .setValue("Edited book");
        test($(form, NumberField.class).id("stock-count")).setValue(100);

        $(MenuButton.class).caption("About").single().click();

        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        assertFalse(form.isShown());

        assertEquals(1, $(AboutView.class).size());
        assertEquals(0, $(BooksView.class).size());
    }

    @Test
    public void editProductDiscardChangesWhenLogout() {
        test(grid).click(1, 0);

        test($(form, TextField.class).id("product-name"))
                .setValue("Edited book");
        test($(form, NumberField.class).id("stock-count")).setValue(100);

        logout();

        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("cancel-button")).click();

        assertEquals(1, $(BooksView.class).size());
        assertTrue(form.isShown());

        // Close form gracefully to avoid side effects
        test($(form, Button.class).id("discard-button")).click();
        test($(form, Button.class).id("cancel-button")).click();
        assertFalse(form.isShown());
    }

    @Test
    public void validationError() {
        test(grid).click(1, 0);

        test($(form, TextField.class).id("product-name")).setValue("");
        test($(form, NumberField.class).id("stock-count")).focus();
        assertTrue(
                test($(form, TextField.class).id("product-name")).isInvalid());
        assertEquals(
                "Product name must have at least two characters and maximum of 100",
                test($(form, TextField.class).id("product-name"))
                        .errorMessage());

        test($(form, Button.class).id("discard-button")).click();
        test($(form, Button.class).id("cancel-button")).click();
        assertFalse(form.isShown());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void categoriesValid() {
        var category = new Category();
        category.setName("Science");
        category = ui.getProductService().updateCategory(category);
        assertNotNull(category.getId());

        test(grid).click(1, 0);

        // Simulate other user deleting the category while editor is
        // open
        ui.getProductService().deleteCategory(category.getId());

        test($(form, CheckBoxGroup.class).id("category")).clickItem(category);
        test($(form, Button.class).id("save-button")).click();

        assertEquals("One or more of the selected categories were deleted.",
                $(Notification.class).last().getCaption());

        test($(form, Button.class).id("cancel-button")).click();
        assertFalse(form.isShown());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void sanitation() {
        // Make window small in order to show tooltip
        ui.getPage().updateBrowserWindowSize(500, 1024, true);

        // Create a book with offending content
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
        test($(form, Button.class).id("save-button")).click();
        assertFalse(form.isShown());

        int row = test(grid).size() - 1;

        // Assert JS sanitized
        assertFalse(test(grid).description(row).contains("alert"));
        // Assert text content remain
        assertTrue(test(grid).description(row).contains("A new book"));

        test(grid).click(1, row);
        var id = test(grid).item(row).getId();
        assertNotNull(id);

        assertTrue(form.isShown());

        test($(form, TextField.class).id("product-name"))
                .setValue("The new book");

        test($(form, Button.class).id("cancel-button")).click();
        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("cancel-button")).click();

        assertTrue($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        // Assert JS sanitized
        assertFalse($(form, TextField.class).id("product-name").getDescription()
                .contains("alert"));
        // Assert text content remain
        assertTrue($(form, TextField.class).id("product-name").getDescription()
                .contains("A new book"));

        test($(form, Button.class).id("discard-button")).click();
        test($(form, Button.class).id("cancel-button")).click();
        assertFalse(form.isShown());

        ui.getProductService().deleteProduct(id);
    }

    @Test
    public void saveEventRefreshes() {
        // Create presenter simulating other user
        var presenter = createBooksPresenter();

        var book = test(grid).item(0);

        // Open second item
        test(grid).click(1, 1);
        assertTrue(form.isShown());

        var name = book.getProductName();
        book.setProductName("Book to be refreshed");

        // Save item in the other presenter, that fires event catched by this
        // view
        var saved = presenter.saveProduct(book);

        // Assert that item was properly refreshed in the event and form is not
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
    public void filterNoMatch() {
        // Search book that does not exists
        test($(FilterField.class).id("filter-field")).setValue("No match");
        assertEquals(0, test(grid).size());
        // Assert that no matches label is shown
        assertTrue($(NoMatches.class)
                .styleName(VaadinCreateTheme.BOOKVIEW_NOMATCHES).single()
                .isVisible());

        // Add a book with matching name
        test($(view, Button.class).id("new-product")).click();
        test($(form, TextField.class).id("product-name"))
                .setValue("No match book");
        test($(form, TextField.class).id("price")).setValue("10.0 €");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);
        test($(form, NumberField.class).id("stock-count")).setValue(10);
        test($(form, Button.class).id("save-button")).click();

        // Assert that no matches label is not shown
        assertEquals(1, test(grid).size());
        assertFalse($(NoMatches.class)
                .styleName(VaadinCreateTheme.BOOKVIEW_NOMATCHES).single()
                .isVisible());

        // Delete the book
        test(grid).click(1, 0);
        test($(form, Button.class).id("delete-button")).click();
        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();
        assertEquals(0, test(grid).size());
        // Assert that no matches label is shown again
        assertTrue($(NoMatches.class)
                .styleName(VaadinCreateTheme.BOOKVIEW_NOMATCHES).single()
                .isVisible());
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
