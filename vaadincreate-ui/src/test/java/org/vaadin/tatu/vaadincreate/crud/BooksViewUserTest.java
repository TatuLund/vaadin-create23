package org.vaadin.tatu.vaadincreate.crud;

import org.junit.After;
import org.junit.Before;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.crud.form.BookForm;

import com.vaadin.server.ServiceException;
import com.vaadin.ui.VerticalLayout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import com.vaadin.ui.Button;

public class BooksViewUserTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private BooksView view;
    private BookGrid grid;
    private BookForm form;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login("User1", "user1");

        view = navigate(BooksView.VIEW_NAME, BooksView.class);

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
    public void _user_can_search_prodcuts_and_new_button_is_disabled_and_clicing_row_does_not_open_form() {
        assertFalse(
                test($(view, Button.class).id("new-product")).isInteractable());

        // WHEN: user searches for debug
        test($(FilterField.class).id("filter-field")).setValue("debug");

        // THEN: only 3 products are shown
        assertEquals(3, test(grid).size());

        // WHEN: user clicks on the first row
        test(grid).click(1, 0);

        // THEN: form is not shown
        assertFalse(form.isShown());
    }
}
