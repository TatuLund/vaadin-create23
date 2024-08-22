package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;

import com.vaadin.server.Page;
import com.vaadin.server.ServiceException;
import com.vaadin.ui.VerticalLayout;

public class BooksViewEditNewUserTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private BooksView view;
    private BookGrid grid;
    private BookForm form;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login("User1", "user1");

        view = navigate(BooksView.VIEW_NAME + "/new", BooksView.class);

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
        assertFalse(form.isShown());
        assertEquals("!inventory/", Page.getCurrent().getUriFragment());
    }

}
