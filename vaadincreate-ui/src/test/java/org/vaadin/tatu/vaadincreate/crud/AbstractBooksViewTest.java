package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.jsoup.Jsoup;
import org.junit.After;
import org.junit.Before;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.common.EuroConverter;
import org.vaadin.tatu.vaadincreate.common.NumberField;
import org.vaadin.tatu.vaadincreate.crud.form.AvailabilitySelector;
import org.vaadin.tatu.vaadincreate.crud.form.BookForm;

import com.vaadin.data.ValueContext;
import com.vaadin.server.ServiceException;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public abstract class AbstractBooksViewTest extends AbstractUITest {

    VaadinCreateUI ui;
    BooksView view;
    BookGrid grid;
    BookForm form;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        view = navigate(BooksView.VIEW_NAME, BooksView.class);
        assertAssistiveNotification("Inventory opened");

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

    @SuppressWarnings("java:S100")
    void then_availability_is_rendered_as_html(int i) {
        var book = test(grid).item(i);
        String color = "";
        switch (book.getAvailability()) {
        case AVAILABLE -> color = VaadinCreateTheme.COLOR_AVAILABLE;
        case DISCONTINUED -> color = VaadinCreateTheme.COLOR_DISCONTINUED;
        case COMING -> color = VaadinCreateTheme.COLOR_COMING;
        }
        var doc = Jsoup.parse((String) test(grid).cell(3, i));
        assertEquals("v-icon",
                doc.getElementsByTag("span").get(0).attr("class"));
        assertEquals(String.format("font-family: Vaadin-Icons;color:%s", color),
                doc.getElementsByTag("span").get(0).attr("style"));
        assertEquals(
                VaadinCreateTheme.BOOKVIEW_AVAILABILITYLABEL + "-aria-label",
                doc.getElementsByTag("span").get(1).attr("class"));
        assertEquals(availability(book),
                doc.getElementsByTag("span").get(1).attr("aria-label"));
        assertEquals(VaadinCreateTheme.BOOKVIEW_AVAILABILITYLABEL,
                doc.getElementsByTag("span").get(2).attr("class"));
        assertEquals(book.getAvailability().toString(),
                doc.getElementsByTag("span").get(2).text());
    }

    private static String availability(Product book) {
        if (book.getAvailability() == Availability.AVAILABLE) {
            return String.format("%s %d", book.getAvailability(),
                    book.getStockCount());
        }
        return book.getAvailability().toString();
    }

    void then_form_is_filled_with_values_from_grid_row(int i) {
        var book = test(grid).item(i);
        assertEquals(book.getProductName(),
                $(form, TextField.class).id("product-name").getValue());
        var price = $(form, TextField.class).id("price");
        var converter = new EuroConverter("");
        assertEquals(
                converter.convertToPresentation(book.getPrice(),
                        new ValueContext(null, price, ui.getLocale())),
                price.getValue());
        assertEquals(book.getStockCount(),
                $(form, NumberField.class).id("stock-count").getValue());

        assertEquals(book.getAvailability(), $(form, AvailabilitySelector.class)
                .id("availability").getValue());
        assertEquals(book.getCategory(),
                $(form, CheckBoxGroup.class).id("category").getValue());
    }

    void then_form_fields_are_reset_state() {
        assertEquals("",
                $(form, TextField.class).id("product-name").getValue());
        assertEquals(Integer.valueOf(0),
                $(form, NumberField.class).id("stock-count").getValue());
        assertEquals("0.00 €", $(form, TextField.class).id("price").getValue());
        assertEquals(Availability.COMING, $(form, AvailabilitySelector.class)
                .id("availability").getValue());
        assertEquals(Collections.emptySet(),
                $(form, CheckBoxGroup.class).id("category").getValue());
    }

    void then_selected_categories_are_shown_first() {
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

    BooksPresenter createBooksPresenter() {
        var bookView = new BooksView();
        // It is not possible to have multiple parallel UIs in this
        // thread
        view.addComponent(bookView);
        var presenter = new BooksPresenter(bookView);
        presenter.requestUpdateProducts();
        waitWhile(() -> $(bookView, FakeGrid.class).first() != null);
        return presenter;
    }

    Product findProductWithPurchaseHistory() {
        var purchases = PurchaseService.get().findAll(0, 500);
        for (var purchase : purchases) {
            for (var line : purchase.getLines()) {
                var lineProduct = line.getProduct();
                if (lineProduct == null || lineProduct.getId() == null) {
                    continue;
                }
                var existing = ui.getProductService()
                        .getProductById(lineProduct.getId());
                if (existing != null) {
                    return existing;
                }
            }
        }
        throw new AssertionError(
                "Expected at least one product referenced by purchase history");
    }

    int findRowByProductId(Integer productId) {
        for (int row = 0; row < test(grid).size(); row++) {
            if (productId.equals(test(grid).item(row).getId())) {
                return row;
            }
        }
        throw new AssertionError("Product not found in grid: " + productId);
    }

    @SuppressWarnings("unchecked")
    void createBook(String name) {
        test($(view, Button.class).id("new-product")).click();
        test($(TextField.class).id("product-name")).setValue(name);
        test($(AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);
        test($(TextField.class).id("price")).setValue("35.0 €");
        var categories = VaadinCreateUI.get().getProductService()
                .getAllCategories().stream().toList();
        test($(CheckBoxGroup.class).id("category"))
                .clickItem(categories.get(1));
        test($(CheckBoxGroup.class).id("category"))
                .clickItem(categories.get(2));
        test($(NumberField.class).id("stock-count")).setValue(100);
        test($(Button.class).id("save-button")).click();
    }
}
