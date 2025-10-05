package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.crud.form.BookForm;

import com.vaadin.server.ServiceException;
import com.vaadin.ui.Button;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;

public class BooksViewIdNotFoundTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private BooksView view;
    private BookGrid grid;
    private BookForm form;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        view = navigate(BooksView.VIEW_NAME + "/12345", BooksView.class);

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
    public void when_books_view_is_entered_with_invalid_product_id_as_url_parameter_form_is_not_shown_and_error_is_shown() {
        assertEquals("Product id \"12345\" not valid or found.",
                $(Notification.class).last().getCaption());
        assertFalse(form.isShown());
        assertTrue($(view, Button.class).id("new-product").isEnabled());
    }
}
