package org.vaadin.tatu.vaadincreate.admin;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.vaadin.tatu.vaadincreate.AbstractViewTest;

import com.vaadin.testbench.elements.ButtonElement;
import com.vaadin.testbench.elements.GridElement;
import com.vaadin.testbench.elements.LabelElement;
import com.vaadin.testbench.elements.MenuBarElement;
import com.vaadin.testbench.elements.NotificationElement;
import com.vaadin.testbench.elements.TextFieldElement;
import com.vaadin.testbench.elements.UIElement;
import com.vaadin.testbench.elements.WindowElement;

public class CategoryManagementViewIT extends AbstractViewTest {

    @Override
    public void setup() throws Exception {
        super.setup();
        open("#!" + AdminView.VIEW_NAME + "/"
                + CategoryManagementView.VIEW_NAME);
        login("Admin", "admin");
    }

    @After
    public void cleanup() {
        $(MenuBarElement.class).first().findElement(By.id("logout-2")).click();
    }

    @Test
    public void addAndRemoveCategory() {
        waitForElementPresent(By.id("new-category"));

        var grid = $(GridElement.class).first();
        var count = grid.getRowCount();
        $(ButtonElement.class).id("new-category").click();
        Assert.assertEquals(count + 1, grid.getRowCount());

        var cell = grid.getCell((int) grid.getRowCount() - 1, 0);
        var nameField = cell.$(TextFieldElement.class).first();
        nameField.sendKeys("Sports");
        var notification = $(NotificationElement.class).last();
        Assert.assertTrue(notification.getText().contains("Sports"));

        cell = grid.getCell((int) grid.getRowCount() - 1, 0);
        cell.$(ButtonElement.class).first().click();
        var dialog = $(WindowElement.class).id("confirm-dialog");
        Assert.assertTrue(dialog.$(LabelElement.class).first().getText()
                .contains("Sports"));
        dialog.$(ButtonElement.class).id("confirm-button").click();
        notification = $(NotificationElement.class).last();
        Assert.assertTrue(notification.getText().contains("Sports"));
        Assert.assertEquals(count, grid.getRowCount());
    }

    @Test
    public void visual() throws IOException {
        waitForElementPresent(By.id("new-category"));

        if (visualTests()) {
            assertTrue(
                    $(UIElement.class).first().compareScreen("category.png"));
        }
    }
}
