package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertFalse;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.vaadin.tatu.vaadincreate.AbstractViewTest;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;

import com.vaadin.testbench.elements.ButtonElement;
import com.vaadin.testbench.elements.CheckBoxGroupElement;
import com.vaadin.testbench.elements.ComboBoxElement;
import com.vaadin.testbench.elements.CssLayoutElement;
import com.vaadin.testbench.elements.GridElement;
import com.vaadin.testbench.elements.LabelElement;
import com.vaadin.testbench.elements.NotificationElement;
import com.vaadin.testbench.elements.TextFieldElement;
import com.vaadin.testbench.elements.VerticalLayoutElement;
import com.vaadin.testbench.elements.WindowElement;
import com.vaadin.ui.themes.ValoTheme;

public class CrudViewIT extends AbstractViewTest {

    @Override
    public void setup() throws Exception {
        super.setup();
        login("Admin", "admin");
        open("#!books");
    }

    @Test
    public void fakeGridShown() {
        waitForElementPresent(By.id("fake-grid"));
        var fakeGrid = $(VerticalLayoutElement.class).id("fake-grid");
        Assert.assertTrue(fakeGrid.$(LabelElement.class).first().getClassNames()
                .contains(ValoTheme.LABEL_SPINNER));
    }

    @Test
    public void saveFindAndDeleteBook() {
        waitForElementPresent(By.id("book-grid"));

        // Part I: Create and save a new book
        $(ButtonElement.class).id("new-product").click();
        var form = $(CssLayoutElement.class).id("book-form");
        Assert.assertFalse(
                form.$(ButtonElement.class).id("save-button").isEnabled());

        form.$(TextFieldElement.class).id("product-name").setValue("Test book");
        form.$(TextFieldElement.class).id("price").setValue("10.00 €");
        form.$(TextFieldElement.class).id("stock-count").setValue("10");
        form.$(ComboBoxElement.class).id("availability")
                .getPopupSuggestionElements().get(1).click();
        form.$(CheckBoxGroupElement.class).id("category")
                .selectByText("Sci-fi");
        Assert.assertTrue("Form input did not enable save button",
                form.$(ButtonElement.class).id("save-button").isEnabled());
        Assert.assertFalse(
                form.$(ButtonElement.class).id("delete-button").isEnabled());

        form.$(ButtonElement.class).id("save-button").click();
        var notification = $(NotificationElement.class).last();
        Assert.assertEquals("Test book (-1) updated", notification.getText());
        notification.close();

        // Book form should close
        Assert.assertFalse(form.getClassNames()
                .contains(VaadinCreateTheme.BOOKFORM_WRAPPER_VISIBLE));

        // Part II: Find it by filter
        $(TextFieldElement.class).id("filter-field").setValue("test");
        var row = $(GridElement.class).first().getRow(0);
        row.click();
        Assert.assertTrue(row.isSelected());

        // Part III: Assert that it is the correct book
        form = $(CssLayoutElement.class).id("book-form");
        Assert.assertTrue(
                form.$(ButtonElement.class).id("delete-button").isEnabled());
        Assert.assertFalse(
                form.$(ButtonElement.class).id("save-button").isEnabled());

        Assert.assertEquals("Test book",
                form.$(TextFieldElement.class).id("product-name").getValue());
        Assert.assertEquals("10.00 €",
                form.$(TextFieldElement.class).id("price").getValue());
        Assert.assertEquals("10",
                form.$(TextFieldElement.class).id("stock-count").getValue());
        Assert.assertEquals("Available",
                form.$(ComboBoxElement.class).id("availability").getValue());
        var category = form.$(CheckBoxGroupElement.class).id("category");
        Assert.assertTrue(category.getValue().contains("Sci-fi"));
        Assert.assertEquals(1, category.getValue().size());

        // Part IV: Delete the book
        form.$(ButtonElement.class).id("delete-button").click();
        var dialog = $(WindowElement.class).id("confirm-dialog");
        Assert.assertTrue(dialog.$(LabelElement.class).first().getText()
                .contains("Test book"));
        Assert.assertTrue(dialog.$(LabelElement.class).first().getText()
                .endsWith("Are you sure?"));
        dialog.$(ButtonElement.class).id("confirm-button").click();

        Assert.assertTrue($(NotificationElement.class).last().getText()
                .contains("removed"));

        Assert.assertEquals(0, $(GridElement.class).first().getRowCount());
    }

    @Test
    public void editDiscardChanges() {
        waitForElementPresent(By.id("book-grid"));
        $(GridElement.class).first().getRow(0).click();
        var form = $(CssLayoutElement.class).id("book-form");
        Assert.assertFalse(
                form.$(ButtonElement.class).id("discard-button").isEnabled());

        var nameField = form.$(TextFieldElement.class).id("product-name");
        nameField.setValue("A renamed book");
        Assert.assertTrue(
                form.$(ButtonElement.class).id("discard-button").isEnabled());

        $(GridElement.class).first().getRow(1).click();
        var dialog = $(WindowElement.class).id("confirm-dialog");
        Assert.assertEquals(
                "There are unsaved changes. Are you sure to change the book?",
                dialog.$(LabelElement.class).first().getText());
        dialog.$(ButtonElement.class).id("confirm-button").click();
    }

    @Test
    public void editedRowHighlighted() {
        waitForElementPresent(By.id("book-grid"));

        // Open first book to editor, change its name
        var row = $(GridElement.class).first().getRow(0);
        row.click();
        var form = $(CssLayoutElement.class).id("book-form");
        Assert.assertTrue(
                form.$(ButtonElement.class).id("delete-button").isEnabled());
        Assert.assertFalse(
                form.$(ButtonElement.class).id("save-button").isEnabled());

        var nameField = form.$(TextFieldElement.class).id("product-name");
        var oldName = nameField.getValue();
        nameField.setValue("A renamed book");
        Assert.assertTrue(
                form.$(ButtonElement.class).id("save-button").isEnabled());

        // Save the book and assert the name was changed in notification
        form.$(ButtonElement.class).id("save-button").click();
        var notification = $(NotificationElement.class).first();
        Assert.assertTrue(notification.getText().startsWith("A renamed book"));
        notification.close();

        // Book form should close
        Assert.assertFalse(form.getClassNames()
                .contains(VaadinCreateTheme.BOOKFORM_WRAPPER_VISIBLE));

        // Assert that row highlight class name exists now
        row = $(GridElement.class).first().getRow(0);
        Assert.assertTrue(row.getClassNames()
                .contains(VaadinCreateTheme.BOOKVIEW_GRID_EDITED));

        // Open book in editor again change name back and assert
        row.click();
        form = $(CssLayoutElement.class).id("book-form");
        nameField = form.$(TextFieldElement.class).id("product-name");
        nameField.setValue(oldName);
        form.$(ButtonElement.class).id("save-button").click();
        notification = $(NotificationElement.class).last();
        Assert.assertTrue(notification.getText().startsWith(oldName));
        notification.close();
    }
}
