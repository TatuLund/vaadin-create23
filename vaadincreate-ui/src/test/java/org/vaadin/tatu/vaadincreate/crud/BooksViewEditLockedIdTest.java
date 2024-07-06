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
import org.vaadin.tatu.vaadincreate.locking.LockedObjects;

import com.vaadin.server.ServiceException;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;

public class BooksViewEditLockedIdTest extends AbstractUITest {

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

        LockedObjects.get().lock(Product.class, 10, CurrentUser.get().get());
        waitForGrid(layout, grid);

        form = $(view, BookForm.class).single();
    }

    @After
    public void cleanUp() {
        LockedObjects.get().unlock(Product.class, 10);
        logout();
        tearDown();
    }

    @Test
    public void editWithLockedIdShowsError() {
        assertEquals("Product id \"10\" is locked.",
                $(Notification.class).last().getCaption());
        assertFalse(form.isShown());
    }

}
