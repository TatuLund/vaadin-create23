package org.vaadin.tatu.vaadincreate.crud;

import org.junit.After;
import org.junit.Before;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.data.User;

import com.vaadin.server.ServiceException;
import com.vaadin.ui.VerticalLayout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Optional;

import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import com.vaadin.ui.Button;
import com.vaadin.ui.TextField;

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
    public void canSearch_newButtonIsDisabled_clickNotOpeningForm() {
        test($(TextField.class).id("filter-field")).setValue("debug");
        assertEquals(3, test(grid).size());

        assertFalse(
                test($(view, Button.class).id("new-product")).isInteractable());

        test(grid).click(1, 0);
        assertFalse(form.isShown());
    }
}
