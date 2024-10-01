package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.backend.data.Product;
import org.vaadin.tatu.vaadincreate.crud.form.BookForm;
import org.vaadin.tatu.vaadincreate.locking.LockedObjects;

import com.vaadin.server.ServiceException;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;

public class BooksViewEditLockedIdTest extends AbstractUITest {

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

        id = getNthProduct(10).getId();
        view = navigate(BooksView.VIEW_NAME + "/" + id, BooksView.class);

        var layout = $(view, VerticalLayout.class).first();
        grid = $(layout, BookGrid.class).single();

        var book = getNthProduct(10);
        LockedObjects.get().lock(book, CurrentUser.get().get());
        waitForGrid(layout, grid);

        form = $(view, BookForm.class).single();
    }

    @After
    public void cleanUp() {
        var book = getNthProduct(10);
        LockedObjects.get().unlock(book);
        logout();
        tearDown();
    }

    @Test
    public void editWithLockedIdShowsError() {
        assertEquals("Product id \"" + id + "\" is locked.",
                $(Notification.class).last().getCaption());
        assertFalse(form.isShown());
    }

    private Product getNthProduct(int n) {
        return ui.getProductService().getAllProducts().stream().skip(n)
                .findFirst().get();
    }

}
