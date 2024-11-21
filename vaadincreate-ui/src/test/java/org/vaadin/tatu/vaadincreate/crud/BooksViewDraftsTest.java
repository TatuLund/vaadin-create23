package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AboutView;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.crud.form.AvailabilitySelector;
import org.vaadin.tatu.vaadincreate.crud.form.BookForm;
import org.vaadin.tatu.vaadincreate.crud.form.NumberField;

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
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
    }

    @Test
    public void editBookExitContinueWithDraft() throws ServiceException {
        createBook("Draft book");
        test($(FilterField.class).id("filter-field")).setValue("Draft book");

        test(grid).click(1, 0);
        var book = test(grid).item(0);
        var id = book.getId();

        assertTrue(form.isShown());

        test($(form, TextField.class).id("product-name"))
                .setValue("Modified book");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);

        // This will close the ui by force, same as closing browser
        tearDown();

        assertNotNull(service.findDraft("Admin"));

        // Start again
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        // Confirm to edit draft
        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        grid = $(BookGrid.class).single();
        waitForGrid((CssLayout) grid.getParent(), grid);
        form = $(BookForm.class).single();
        assertTrue(form.isShown());
        assertNull(service.findDraft("Admin"));

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

        test($(form, NumberField.class).id("stock-count")).setValue(200);

        test($(form, Button.class).id("save-button")).click();
        assertTrue($(Notification.class).last().getCaption()
                .contains("Modified book"));

        assertFalse(form.isShown());

        ui.getProductService().deleteProduct(id);
    }

    @Test
    public void editNewExitContinueWithDraft() throws ServiceException {
        test($(view, Button.class).id("new-product")).click();
        assertTrue(form.isShown());

        test($(form, TextField.class).id("product-name"))
                .setValue("Modified book");
        test($(form, NumberField.class).id("stock-count")).setValue(200);
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.DISCONTINUED);

        // This will close the ui by force, same as closing browser
        tearDown();

        assertNotNull(service.findDraft("Admin"));

        // Start again
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        // Confirm to edit draft
        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        grid = $(BookGrid.class).single();
        waitForGrid((CssLayout) grid.getParent(), grid);
        form = $(BookForm.class).single();
        assertTrue(form.isShown());
        assertNull(service.findDraft("Admin"));

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

        test($(form, NumberField.class).id("stock-count")).setValue(0);

        test($(form, Button.class).id("save-button")).click();
        assertTrue($(Notification.class).last().getCaption()
                .contains("Modified book"));

        assertFalse(form.isShown());

        var book = test(grid).item(test(grid).size() - 1);
        assertEquals("Modified book", book.getProductName());

        var id = book.getId();
        ui.getProductService().deleteProduct(id);
    }

    @Test
    public void editBookExitConcurrentEditContinueWithDraft()
            throws ServiceException {
        createBook("Draft book");
        test($(FilterField.class).id("filter-field")).setValue("Draft book");

        test(grid).click(1, 0);
        var book = test(grid).item(0);
        assertTrue(form.isShown());

        test($(form, TextField.class).id("product-name"))
                .setValue("Modified book");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);

        // This will close the ui by force, same as closing browser
        tearDown();

        assertNotNull(service.findDraft("Admin"));

        // Simulate other user editing book
        var edited = service.getProductById(book.getId());
        edited.setProductName("Edited book");
        edited.setStockCount(300);
        service.updateProduct(edited);

        // Start again
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        // Confirm to edit draft
        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        grid = $(BookGrid.class).single();
        waitForGrid((CssLayout) grid.getParent(), grid);
        form = $(BookForm.class).single();
        assertTrue(form.isShown());
        assertNull(service.findDraft("Admin"));

        assertEquals("Modified book",
                $(form, TextField.class).id("product-name").getValue());
        assertTrue($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertTrue($(form, TextField.class).id("product-name").getDescription()
                .contains("Edited book"));

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

        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.DISCONTINUED);

        test($(form, Button.class).id("save-button")).click();
        assertTrue($(Notification.class).last().getCaption()
                .contains("Modified book"));

        assertFalse(form.isShown());

        book = test(grid).item(test(grid).size() - 1);
        assertEquals("Modified book", book.getProductName());

        var id = book.getId();
        ui.getProductService().deleteProduct(id);
    }

    @Test
    public void editBookExitConcurrentDeleteContinueWithDraft()
            throws ServiceException {
        createBook("Draft book");
        test($(FilterField.class).id("filter-field")).setValue("Draft book");

        test(grid).click(1, 0);
        var book = test(grid).item(0);
        assertTrue(form.isShown());

        test($(form, TextField.class).id("product-name"))
                .setValue("Modified book");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);

        // This will close the ui by force, same as closing browser
        tearDown();

        assertNotNull(service.findDraft("Admin"));

        // Simulate other user editing book
        service.deleteProduct(book.getId());

        // Start again
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        // Confirm to edit draft
        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        grid = $(BookGrid.class).single();
        waitForGrid((CssLayout) grid.getParent(), grid);
        form = $(BookForm.class).single();
        assertTrue(form.isShown());
        assertNull(service.findDraft("Admin"));

        assertEquals("Product was deleted.",
                $(Notification.class).last().getCaption());

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

        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.DISCONTINUED);

        test($(form, Button.class).id("save-button")).click();
        assertTrue($(Notification.class).last().getCaption()
                .contains("Modified book"));

        assertFalse(form.isShown());

        book = test(grid).item(test(grid).size() - 1);
        assertEquals("Modified book", book.getProductName());

        var id = book.getId();
        ui.getProductService().deleteProduct(id);
    }

    @Test
    public void leaveDraftCancelDraft() throws ServiceException {
        test(grid).click(1, 0);
        assertTrue(form.isShown());

        test($(form, TextField.class).id("product-name"))
                .setValue("Modified book");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);

        // This will close the ui by force, same as closing browser
        tearDown();

        assertNotNull(service.findDraft("Admin"));

        // Start again
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        // Confirm to edit draft
        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("cancel-button")).click();

        assertNull(service.findDraft("Admin"));

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
                .getAllCategories().stream().collect(Collectors.toList());
        test($(CheckBoxGroup.class).id("category"))
                .clickItem(categories.get(1));
        test($(CheckBoxGroup.class).id("category"))
                .clickItem(categories.get(2));
        test($(NumberField.class).id("stock-count")).setValue(0);
        test($(Button.class).id("save-button")).click();
    }
}
