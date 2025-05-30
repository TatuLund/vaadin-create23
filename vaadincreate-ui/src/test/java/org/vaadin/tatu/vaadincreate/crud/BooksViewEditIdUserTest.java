package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.crud.form.BookForm;

import com.vaadin.server.Page;
import com.vaadin.server.ServiceException;
import com.vaadin.ui.VerticalLayout;

public class BooksViewEditIdUserTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private BooksView view;
    private BookGrid grid;
    private BookForm form;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login("User1", "user1");

        var id = getNthProduct(10).getId();
        view = navigate(BooksView.VIEW_NAME + "/" + id, BooksView.class);

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
    public void when_user_opens_books_view_with_product_id_as_parameter_form_is_not_being_shown_and_parameter_is_stripped() {
        assertFalse(form.isShown());
        assertEquals("!inventory/", Page.getCurrent().getUriFragment());
    }

    private Product getNthProduct(int n) {
        return ui.getProductService().getAllProducts().stream().skip(n)
                .findFirst().get();
    }

}
