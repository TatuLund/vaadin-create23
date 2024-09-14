package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AboutView;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Product;

import com.vaadin.server.ServiceException;
import com.vaadin.ui.Button;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class BooksViewDraftsTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private Collection<Product> backup;
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

        backup = service.backup();

        view = navigate(BooksView.VIEW_NAME, BooksView.class);

        layout = $(view, VerticalLayout.class).first();
        grid = $(layout, BookGrid.class).single();
        waitForGrid(layout, grid);
        form = $(view, BookForm.class).single();
    }

    @After
    public void cleanUp() {
        service.restore(backup);
        logout();
        tearDown();
    }

    @Test
    public void editBookExitContinueWithDraft() throws ServiceException {
        test(grid).click(1, 0);
        var book = test(grid).item(0);

        assertTrue(form.isShown());

        test(form.productName).setValue("Modified book");
        test(form.stockCount).setValue("200");
        test(form.availability).clickItem(Availability.COMING);

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
        waitForGrid((VerticalLayout) grid.getParent(), grid);
        form = $(BookForm.class).single();
        assertTrue(form.isShown());
        assertNull(service.findDraft("Admin"));

        assertEquals("Modified book", form.productName.getValue());
        assertEquals("200", form.stockCount.getValue());
        assertEquals(Availability.COMING, form.availability.getValue());
        assertEquals(book.getCategory(), form.category.getValue());

        test(form.availability).clickItem(Availability.AVAILABLE);

        test(form.saveButton).click();
        assertTrue($(Notification.class).last().getCaption()
                .contains("Modified book"));

        assertFalse(form.isShown());
    }

    @Test
    public void editNewExitContinueWithDraft() throws ServiceException {
        test($(view, Button.class).id("new-product")).click();
        assertTrue(form.isShown());

        test(form.productName).setValue("Modified book");
        test(form.stockCount).setValue("200");
        test(form.availability).clickItem(Availability.COMING);

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
        waitForGrid((VerticalLayout) grid.getParent(), grid);
        form = $(BookForm.class).single();
        assertTrue(form.isShown());
        assertNull(service.findDraft("Admin"));

        assertEquals("Modified book", form.productName.getValue());
        assertEquals("200", form.stockCount.getValue());
        assertEquals(Availability.COMING, form.availability.getValue());
        assertTrue(form.category.getValue().isEmpty());

        test(form.stockCount).setValue("0");

        test(form.saveButton).click();
        assertTrue($(Notification.class).last().getCaption()
                .contains("Modified book"));

        assertFalse(form.isShown());
    }

    @Test
    public void editBookExitConcurrentEditContinueWithDraft()
            throws ServiceException {
        test(grid).click(1, 0);
        var book = test(grid).item(0);
        assertTrue(form.isShown());

        test(form.productName).setValue("Modified book");
        test(form.availability).clickItem(Availability.AVAILABLE);

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
        waitForGrid((VerticalLayout) grid.getParent(), grid);
        form = $(BookForm.class).single();
        assertTrue(form.isShown());
        assertNull(service.findDraft("Admin"));

        assertEquals("Modified book", form.productName.getValue());
        assertEquals("0", form.stockCount.getValue());
        assertEquals(Availability.AVAILABLE, form.availability.getValue());
        assertEquals(book.getCategory(), form.category.getValue());

        test(form.availability).clickItem(Availability.DISCONTINUED);

        test(form.saveButton).click();
        assertTrue($(Notification.class).last().getCaption()
                .contains("Modified book"));

        assertFalse(form.isShown());
    }

    @Test
    public void leaveDraftCancelDraft() throws ServiceException {
        test(grid).click(1, 0);
        assertTrue(form.isShown());

        test(form.productName).setValue("Modified book");
        test(form.availability).clickItem(Availability.AVAILABLE);

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
}
