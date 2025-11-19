package org.vaadin.tatu.vaadincreate.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;
import org.vaadin.tatu.vaadincreate.admin.CategoryForm.NameField;
import org.vaadin.tatu.vaadincreate.backend.data.Category;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.ServiceException;
import com.vaadin.testbench.uiunittest.SerializationDebugUtil;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Window;

public class CategoryManagementViewTest extends AbstractUITest {

    private VaadinCreateUI ui;
    private AdminView view;
    private CategoryManagementView cats;
    private TabSheet tabs;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        view = navigate(AdminView.VIEW_NAME, AdminView.class);
        assertAssistiveNotification("Categories opened");

        tabs = $(view, TabSheet.class).first();
        cats = (CategoryManagementView) test(tabs).current();
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
    }

    @Test
    public void create_and_delete_category() {
        var newCategoryButton = $(cats, Button.class).id("new-category");
        test(newCategoryButton).isFocused();
        test(newCategoryButton).click();
        assertFalse(newCategoryButton.isEnabled());

        @SuppressWarnings("unchecked")
        var grid = (Grid<Category>) $(cats, Grid.class).single();
        var gridSize = test(grid).size();
        var form = (CategoryForm) test(grid).cell(0, gridSize - 1);
        test($(form, NameField.class).first()).setValue("Tech horror");

        var cat = test(grid).item(gridSize - 1);
        assertEquals("Tech horror", cat.getName());
        assertEquals("Category \"Tech horror\" saved.",
                $(Notification.class).last().getCaption());

        assertTrue(ui.getProductService().getAllCategories().stream()
                .anyMatch(c -> c.getName().equals("Tech horror")));

        var newCat = ui.getProductService().getAllCategories().stream()
                .filter(c -> c.getName().equals("Tech horror")).findFirst()
                .get();

        form = (CategoryForm) test(grid).cell(0, gridSize - 1);
        test($(form, NameField.class).first()).setValue("Technology");
        assertEquals("Category \"Technology\" saved.",
                $(Notification.class).last().getCaption());
        cat = test(grid).item(gridSize - 1);
        assertEquals("Technology", cat.getName());

        form = (CategoryForm) test(grid).cell(0, gridSize - 1);
        test($(form, Button.class).first()).click();
        assertTrue($(cats, Button.class).id("new-category").isEnabled());
        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();
        assertEquals("Category \"Technology\" removed.",
                $(Notification.class).last().getCaption());

        assertFalse(ui.getProductService().getAllCategories().contains(newCat));
    }

    @Test
    public void updating_category_updated_by_other_user_will_show_save_conflict_and_will_refresh_the_list() {
        test($(cats, Button.class).id("new-category")).click();
        assertFalse($(cats, Button.class).id("new-category").isEnabled());

        @SuppressWarnings("unchecked")
        var grid = (Grid<Category>) $(cats, Grid.class).single();
        var gridSize = test(grid).size();
        var form = (CategoryForm) test(grid).cell(0, gridSize - 1);
        test($(form, NameField.class).first()).setValue("Tech horror");

        var cat = test(grid).item(gridSize - 1);
        assertEquals("Tech horror", cat.getName());
        assertEquals("Category \"Tech horror\" saved.",
                $(Notification.class).last().getCaption());

        assertTrue(ui.getProductService().getAllCategories().stream()
                .anyMatch(c -> c.getName().equals("Tech horror")));

        var newCat = ui.getProductService().getAllCategories().stream()
                .filter(c -> c.getName().equals("Tech horror")).findFirst()
                .get();
        newCat.setName("Soft horror");
        ui.getProductService().updateCategory(newCat);

        form = (CategoryForm) test(grid).cell(0, gridSize - 1);
        test($(form, NameField.class).first()).setValue("Technology");
        // Assert that optimistic locking is thrown and caught
        assertEquals("Save conflict, try again.",
                $(Notification.class).last().getCaption());

        cat = test(grid).item(gridSize - 1);
        assertEquals("Soft horror", cat.getName());
        assertTrue($(cats, Button.class).id("new-category").isEnabled());

        form = (CategoryForm) test(grid).cell(0, gridSize - 1);
        test($(form, Button.class).first()).click();
        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();
        assertEquals("Category \"Soft horror\" removed.",
                $(Notification.class).last().getCaption());
    }

    @Test
    public void input_duplicate_category_will_show_duplicate_category_validation_error() {
        test($(cats, Button.class).id("new-category")).click();
        @SuppressWarnings("unchecked")
        var grid = (Grid<Category>) $(cats, Grid.class).single();
        var gridSize = test(grid).size();
        var form = (CategoryForm) test(grid).cell(0, gridSize - 1);
        test($(form, NameField.class).first()).setValue("Sci-fi");
        assertTrue(test($(form, NameField.class).first()).isInvalid());
        assertEquals("Category's name already in use.",
                test($(form, NameField.class).first()).errorMessage());
    }

    @Test
    public void input_too_short_name_will_show_category_length_validation_error() {
        test($(cats, Button.class).id("new-category")).click();
        @SuppressWarnings("unchecked")
        var grid = (Grid<Category>) $(cats, Grid.class).single();
        var gridSize = test(grid).size();
        var form = (CategoryForm) test(grid).cell(0, gridSize - 1);
        test($(form, NameField.class).first()).setValue("Sci");
        assertTrue(test($(form, NameField.class).first()).isInvalid());
        assertEquals("Category length is at least 5 and max 40 characters",
                test($(form, NameField.class).first()).errorMessage());
    }

    @Test
    public void new_category_button_is_enabled_after_tab_change() {
        test($(cats, Button.class).id("new-category")).click();
        @SuppressWarnings("unchecked")
        var grid = (Grid<Category>) $(cats, Grid.class).single();
        var gridSize = test(grid).size();
        var form = (CategoryForm) test(grid).cell(0, gridSize - 1);
        test($(form, NameField.class).first()).setValue("Sci");

        test(tabs).click(1);
        test(tabs).click(0);

        assertTrue($(cats, Button.class).id("new-category").isEnabled());
    }

    @Test
    public void updating_category_deleted_by_other_user_will_show_save_conflict_error() {
        // Simulate other user saving category
        Category cat = new Category();
        cat.setName("Horrors");
        var newCat = ui.getProductService().updateCategory(cat);

        // Switch to user tab and back, will update
        test(tabs).click(1);
        test(tabs).click(0);

        @SuppressWarnings("unchecked")
        var grid = (Grid<Category>) $(cats, Grid.class).single();
        var gridSize = test(grid).size();
        var form = (CategoryForm) test(grid).cell(0, gridSize - 1);
        assertEquals("Horrors", $(form, NameField.class).first().getValue());

        // Simulate other user deleting the category
        ui.getProductService().deleteCategory(newCat.getId());

        // Attempt to edit the category
        test($(form, NameField.class).first()).setValue("Horror");

        // Assert that optimistic locking is thrown and caught
        assertEquals("Save conflict, try again.",
                $(Notification.class).last().getCaption());
        assertTrue($(cats, Button.class).id("new-category").isEnabled());
    }

    @Test
    public void focusing_namefield_will_disable_new_category_button() {
        assertTrue($(cats, Button.class).id("new-category").isEnabled());
        @SuppressWarnings("unchecked")
        var grid = (Grid<Category>) $(cats, Grid.class).single();
        var form = (CategoryForm) test(grid).cell(0, 0);
        test($(form, NameField.class).first()).focus();
        assertFalse($(cats, Button.class).id("new-category").isEnabled());
        test($(form, Button.class).first()).focus();
        assertTrue($(cats, Button.class).id("new-category").isEnabled());
    }

    @Test
    public void pressing_Esc_will_close_editor() {
        assertTrue($(cats, Button.class).id("new-category").isEnabled());
        @SuppressWarnings("unchecked")
        var grid = (Grid<Category>) $(cats, Grid.class).single();
        var form = (CategoryForm) test(grid).cell(0, 0);
        var name = $(form, NameField.class).first();
        test(name).focus();
        assertFalse($(cats, Button.class).id("new-category").isEnabled());
        test(cats).shortcut(KeyCode.ESCAPE);
        assertTrue($(cats, Button.class).id("new-category").isEnabled());
    }

    @Test
    public void concurrent_adding_of_duplicate_category_will_show_error() {
        test($(cats, Button.class).id("new-category")).click();

        // Simulate other user saving category while this view is open
        var category = new Category();
        category.setName("Duplicate");
        category = ui.getProductService().updateCategory(category);

        // Attempt adding category with the same name as the other user saved
        @SuppressWarnings("unchecked")
        var grid = (Grid<Category>) $(cats, Grid.class).single();
        var gridSize = test(grid).size();
        var form = (CategoryForm) test(grid).cell(0, gridSize - 1);
        test($(form, NameField.class).first()).setValue("Duplicate");

        // Assert that save failed and error shown
        assertEquals("Save conflict, try again.",
                $(Notification.class).last().getCaption());

        // Clean-up
        ui.getProductService().deleteCategory(category.getId());
    }

    @Test
    public void concurrent_delete_will_show_error() {
        // Save a new category "Deleted"
        test($(cats, Button.class).id("new-category")).click();
        @SuppressWarnings("unchecked")
        var grid = (Grid<Category>) $(cats, Grid.class).single();
        var gridSize = test(grid).size();
        var form = (CategoryForm) test(grid).cell(0, gridSize - 1);
        test($(form, NameField.class).first()).setValue("Deleted");

        assertEquals("Category \"Deleted\" saved.",
                $(Notification.class).last().getCaption());
        var category = test(grid).item(gridSize - 1);
        assertEquals("Deleted", category.getName());

        // Simulate other user deleting the category concurrently
        ui.getProductService().deleteCategory(category.getId());

        // Attempt to delete the category
        form = (CategoryForm) test(grid).cell(0, gridSize - 1);
        test($(form, Button.class).first()).click();
        var dialog = $(Window.class).id("confirm-dialog");
        test($(dialog, Button.class).id("confirm-button")).click();

        // Assert that delete failed and error shown
        assertEquals("Category was already deleted.",
                $(Notification.class).last().getCaption());
    }

    @Test
    public void category_management_view_is_serializable() {
        SerializationDebugUtil.assertSerializable(view);
    }
}
