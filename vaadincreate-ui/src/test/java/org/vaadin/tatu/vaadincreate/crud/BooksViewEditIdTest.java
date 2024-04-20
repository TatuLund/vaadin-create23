package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;

import com.vaadin.data.ValueContext;
import com.vaadin.server.ServiceException;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class BooksViewEditIdTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private BooksView view;
    private BookGrid grid;
    private BookForm form;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        view = navigate(BooksView.VIEW_NAME + "/10", BooksView.class);

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
    public void editWithId() {
        var product = ui.getProductService().getProductById(10);
        assertEquals(product, grid.asSingleSelect().getSelectedItem().get());

        assertTrue(form.isShown());
        assertEquals(product.getProductName(), form.productName.getValue());
        var price = form.price;
        var converter = new EuroConverter("");
        var priceString = converter.convertToPresentation(product.getPrice(),
                new ValueContext(null, price, ui.getLocale()));
        assertEquals(priceString, form.price.getValue());
        assertEquals("" + product.getStockCount(), form.stockCount.getValue());
        assertEquals(product.getAvailability(), form.availability.getValue());
        assertEquals(product.getCategory(), form.category.getValue());

        test(form.productName).setValue("Modified book");
        test(form.price).setValue("10.0 €");
        test(form.availability).clickItem(Availability.AVAILABLE);
        test(form.stockCount).setValue("10");

        var cat = ui.getProductService().getAllCategories().stream().findFirst()
                .get();
        test(form.category).clickItem(cat);

        test(form.save).click();

        assertTrue($(Notification.class).last().getCaption()
                .contains("Modified book"));

        var savedProduct = ui.getProductService().getProductById(10);
        assertEquals("Modified book", savedProduct.getProductName());

    }

}
