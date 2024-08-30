package org.vaadin.tatu.vaadincreate.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.data.Category;

import com.vaadin.server.ServiceException;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class CategoryManagementViewTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private AdminView admin;
    private CategoryManagementView cats;
    private TabSheet tabs;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        admin = navigate(AdminView.VIEW_NAME, AdminView.class);
        tabs = $(admin, TabSheet.class).first();
        cats = (CategoryManagementView) test(tabs).current();
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
    }

    @Test
    public void createAndDeleteCategory() throws ServiceException {
        test($(cats, Button.class).id("new-category")).click();
        assertFalse($(cats, Button.class).id("new-category").isEnabled());

        @SuppressWarnings("unchecked")
        var grid = (Grid<Category>) $(cats, Grid.class).single();
        var gridSize = test(grid).size();
        var horiz = (HorizontalLayout) test(grid).cell(0, gridSize - 1);
        test($(horiz, TextField.class).first()).setValue("Tech horror");

        var cat = test(grid).item(gridSize - 1);
        assertEquals("Tech horror", cat.getName());

        assertTrue(ui.getProductService().getAllCategories().stream()
                .anyMatch(c -> c.getName().equals("Tech horror")));

        var newCat = ui.getProductService().getAllCategories().stream()
                .filter(c -> c.getName().equals("Tech horror")).findFirst()
                .get();

        horiz = (HorizontalLayout) test(grid).cell(0, gridSize - 1);
        test($(horiz, Button.class).first()).click();
        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        assertFalse(ui.getProductService().getAllCategories().contains(newCat));
    }

    @Test
    public void duplicateCategory() {
        test($(cats, Button.class).id("new-category")).click();
        @SuppressWarnings("unchecked")
        var grid = (Grid<Category>) $(cats, Grid.class).single();
        var gridSize = test(grid).size();
        var horiz = (HorizontalLayout) test(grid).cell(0, gridSize - 1);
        test($(horiz, TextField.class).first()).setValue("Sci-fi");
        assertTrue(test($(horiz, TextField.class).first()).isInvalid());
        assertEquals("Category's name already in use.",
                test($(horiz, TextField.class).first()).errorMessage());
    }

    @Test
    public void categoryLength() {
        test($(cats, Button.class).id("new-category")).click();
        @SuppressWarnings("unchecked")
        var grid = (Grid<Category>) $(cats, Grid.class).single();
        var gridSize = test(grid).size();
        var horiz = (HorizontalLayout) test(grid).cell(0, gridSize - 1);
        test($(horiz, TextField.class).first()).setValue("Sci");
        assertTrue(test($(horiz, TextField.class).first()).isInvalid());
        assertEquals("Category length is at least 5 and max 40 characters",
                test($(horiz, TextField.class).first()).errorMessage());
    }

    @Test
    public void newButtonEnabledAfterTabChange() {
        test($(cats, Button.class).id("new-category")).click();
        @SuppressWarnings("unchecked")
        var grid = (Grid<Category>) $(cats, Grid.class).single();
        var gridSize = test(grid).size();
        var horiz = (HorizontalLayout) test(grid).cell(0, gridSize - 1);
        test($(horiz, TextField.class).first()).setValue("Sci");

        test(tabs).click(1);
        test(tabs).click(0);

        assertTrue($(cats, Button.class).id("new-category").isEnabled());
    }
}
