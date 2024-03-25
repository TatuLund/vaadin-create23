package org.vaadin.tatu.vaadincreate.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.backend.ProductDataService;
import org.vaadin.tatu.vaadincreate.backend.data.Category;

import com.vaadin.server.ServiceException;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class CategoryManagementViewTest extends AbstractUITest {

    @Test
    public void createAndDeleteCategory() throws ServiceException {
        var ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        var admin = (AdminView) navigate(AdminView.VIEW_NAME);

        var tabs = $(admin, TabSheet.class).first();
        var cats = (CategoryManagementView) tabs.getSelectedTab();
        var newCategory = $(cats, Button.class).id("new-category");
        newCategory.click();
        var grid = (Grid<Category>) $(cats, Grid.class).first();
        var gridSize = getGridSize(grid);
        var horiz = (HorizontalLayout) getGridCell(grid, 0, gridSize - 1);
        var catField = $(horiz, TextField.class).first();
        catField.setValue("Tech horror");

        var cat = (Category) getGridItem(grid, gridSize - 1);
        assertEquals("Tech horror", cat.getName());

        assertTrue(ProductDataService.get().getAllCategories().stream()
                .anyMatch(c -> c.getName().equals("Tech horror")));

        var newCat = ProductDataService.get().getAllCategories().stream()
                .filter(c -> c.getName().equals("Tech horror")).findFirst()
                .get();

        horiz = (HorizontalLayout) getGridCell(grid, 0, gridSize - 1);
        var deleteButton = $(horiz, Button.class).first();
        deleteButton.click();
        var dialog = $(Window.class).id("confirm-dialog");
        $(dialog, Button.class).id("confirm-button").click();

        assertFalse(
                ProductDataService.get().getAllCategories().contains(newCat));

        tearDown();
    }

}
