package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Product;

import com.vaadin.server.ServiceException;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;

public class BooksViewNewTest extends AbstractUITest {

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

        view = navigate(BooksView.VIEW_NAME + "/new", BooksView.class);

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
    public void newProduct() {
        test(form.productName).setValue("A new product");
        test(form.price).setValue("10.0 €");
        test(form.availability).clickItem(Availability.AVAILABLE);
        test(form.stockCount).setValue("10");

        var cat = ui.getProductService().getAllCategories().stream().findFirst()
                .get();
        test(form.category).clickItem(cat);

        test(form.save).click();

        assertTrue($(Notification.class).last().getCaption()
                .contains("A new product"));

        assertTrue(ui.getProductService().getAllProducts().stream()
                .anyMatch(b -> b.getProductName().equals("A new product")));

        int row = test(grid).size() - 1;
        assertEquals("A new product", test(grid).cell(1, row));
        assertEquals("10.00 €", test(grid).cell(2, row));
        assertEquals("10", test(grid).cell(4, row));
    }

}
