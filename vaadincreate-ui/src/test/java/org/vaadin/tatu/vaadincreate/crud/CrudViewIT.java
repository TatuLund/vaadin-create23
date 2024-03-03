package org.vaadin.tatu.vaadincreate.crud;

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

    private final static String PRICE_EN = "10.00 €";
    private final static String PRICE_FI = "10,00 €";

    @Override
    public void setup() throws Exception {
        super.setup();
        login("Admin", "admin");
        open("#!" + BooksView.VIEW_NAME);
    }

    @Test
    public void fakeGridShown() {
        waitForElementPresent(By.id("fake-grid"));
        var fakeGrid = $(VerticalLayoutElement.class).id("fake-grid");
        Assert.assertTrue(fakeGrid.$(LabelElement.class).first().getClassNames()
                .contains(ValoTheme.LABEL_SPINNER));
    }

    private String getLocale() {
        return (String) executeScript(
                "return window.navigator.userLanguage || window.navigator.language");
    }

    @Test
    public void saveFindAndDeleteBook() {
        waitForElementPresent(By.id("book-grid"));

        String locale = getLocale();
        String price = "";
        if (locale.contains("en")) {
            price = PRICE_EN;
        } else {
            price = PRICE_FI;
        }

        // Part I: Create and save a new book
        $(ButtonElement.class).id("new-product").click();
        var form = $(CssLayoutElement.class).id("book-form");
        Assert.assertFalse(
                form.$(ButtonElement.class).id("save-button").isEnabled());

        form.$(TextFieldElement.class).id("product-name").setValue("Test book");

        form.$(TextFieldElement.class).id("price").setValue(price);
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
        Assert.assertTrue(notification.getText().contains("Test book"));
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
        Assert.assertEquals(price,
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
        dialog.$(ButtonElement.class).id("confirm-button").click();

        Assert.assertTrue($(NotificationElement.class).last().getText()
                .contains("Test book"));

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
        var oldName = nameField.getValue();

        nameField.setValue("A renamed book");
        var discard = form.$(ButtonElement.class).id("discard-button");
        Assert.assertTrue(discard.isEnabled());
        discard.click();

        Assert.assertEquals(oldName, nameField.getValue());
    }

    @Test
    public void editDiscardChangesWarningOnSelect() {
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
        Assert.assertTrue(
                dialog.$(LabelElement.class).first().getText().length() > 0);
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
        nameField.setValue("A changed book");
        Assert.assertTrue(
                form.$(ButtonElement.class).id("save-button").isEnabled());

        // Save the book and assert the name was changed in notification
        form.$(ButtonElement.class).id("save-button").click();
        var notification = $(NotificationElement.class).first();
        Assert.assertTrue(notification.getText().contains("A changed book"));
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
        Assert.assertTrue(notification.getText().contains(oldName));
        notification.close();
    }

    @Test
    public void browseProducts() {
        waitForElementPresent(By.id("book-grid"));

        // Click first book and pick the name of it
        var row = $(GridElement.class).first().getRow(0);
        row.click();
        var form = $(CssLayoutElement.class).id("book-form");
        var nameField = form.$(TextFieldElement.class).id("product-name");
        var oldName = nameField.getValue();
        // Compare values to Grid row
        Assert.assertEquals(oldName, row.getCell(0).getText());
        Assert.assertEquals(
                form.$(TextFieldElement.class).id("price").getValue(),
                row.getCell(1).getText());
        Assert.assertTrue(row.getCell(2).getText().endsWith(
                form.$(ComboBoxElement.class).id("availability").getText()));
        Assert.assertEquals(
                form.$(TextFieldElement.class).id("stock-count").getValue(),
                stockCount(row.getCell(3).getText()));

        // Click the second book
        row = $(GridElement.class).first().getRow(1);
        row.click();
        // Form is still open
        Assert.assertTrue(form.getClassNames()
                .contains(VaadinCreateTheme.BOOKFORM_WRAPPER_VISIBLE));
        nameField = form.$(TextFieldElement.class).id("product-name");
        // Name should be different
        Assert.assertNotEquals(oldName, nameField.getValue());
        // Values should match Grid row
        Assert.assertEquals(nameField.getValue(), row.getCell(0).getText());
        Assert.assertEquals(
                form.$(TextFieldElement.class).id("price").getValue(),
                row.getCell(1).getText());
        Assert.assertTrue(row.getCell(2).getText().endsWith(
                form.$(ComboBoxElement.class).id("availability").getText()));
        Assert.assertEquals(
                form.$(TextFieldElement.class).id("stock-count").getValue(),
                stockCount(row.getCell(3).getText()));
    }

    private static String stockCount(String count) {
        if (count.equals("-"))
            return "0";
        return count;
    }
}
