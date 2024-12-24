package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.crud.form.AvailabilitySelector;
import org.vaadin.tatu.vaadincreate.crud.form.BookForm;
import org.vaadin.tatu.vaadincreate.crud.form.NumberField;
import org.vaadin.tatu.vaadincreate.locking.LockedObjects;

import com.vaadin.data.ValueContext;
import com.vaadin.server.ServiceException;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class BooksViewEditIdTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private BooksView view;
    private BookGrid grid;
    private BookForm form;
    private Integer id;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        var book = createBook();
        book = ui.getProductService().updateProduct(book);
        id = book.getId();

        view = navigate(BooksView.VIEW_NAME + "/" + id, BooksView.class);

        var layout = $(view, VerticalLayout.class).first();
        grid = $(layout, BookGrid.class).single();
        waitForGrid(layout, grid);
        form = $(view, BookForm.class).single();

    }

    @After
    public void cleanUp() {
        ui.getProductService().deleteProduct(id);
        logout();
        tearDown();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void when_opening_applicaiton_with_product_id_url_parameter_will_open_books_view_with_form_open_with_given_product_id_and_product_can_be_modified_and_saved() {
        // THEN: The form is shown, grid is focused and the product in the grid
        // is selected, and the form is populated with the product data, and the
        // product is locked
        var product = ui.getProductService().getProductById(id);
        assertNotNull(LockedObjects.get().isLocked(product));

        assertEquals(product, grid.asSingleSelect().getSelectedItem().get());

        assertTrue(form.isShown());
        assertTrue(test(grid).isFocused());
        assertEquals(product.getProductName(),
                $(form, TextField.class).id("product-name").getValue());
        var price = $(form, TextField.class).id("price");
        var converter = new EuroConverter("");
        var priceString = converter.convertToPresentation(product.getPrice(),
                new ValueContext(null, price, ui.getLocale()));
        assertEquals(priceString,
                $(form, TextField.class).id("price").getValue());
        assertEquals(product.getStockCount(),
                $(form, NumberField.class).id("stock-count").getValue());
        assertEquals(product.getAvailability(),
                $(form, AvailabilitySelector.class).id("availability")
                        .getValue());
        assertEquals(product.getCategory(),
                $(form, CheckBoxGroup.class).id("category").getValue());

        // WHEN: Modifying the product and clicking save button
        test($(form, TextField.class).id("product-name"))
                .setValue("Modified book");
        test($(form, TextField.class).id("price")).setValue("10.0 €");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);
        test($(form, NumberField.class).id("stock-count")).setValue(10);

        var cat = ui.getProductService().getAllCategories().stream().findFirst()
                .get();
        test($(form, CheckBoxGroup.class).id("category")).clickItem(cat);

        test($(form, Button.class).id("save-button")).click();

        // THEN: The form is hidden, a notification is shown, the product is
        // saved with the new data
        assertTrue($(Notification.class).last().getCaption()
                .contains("Modified book"));
        assertFalse(form.isShown());

        var savedProduct = ui.getProductService().getProductById(id);
        assertEquals("Modified book", savedProduct.getProductName());
    }

    private static Product createBook() {
        var book = new Product();
        book.setProductName("Test book");
        book.setAvailability(Availability.COMING);
        var categories = VaadinCreateUI.get().getProductService()
                .findCategoriesByIds(Set.of(1, 2));
        book.setCategory(categories);
        book.setStockCount(0);
        book.setPrice(BigDecimal.valueOf(35));
        return book;
    }
}
