package org.vaadin.tatu.vaadincreate.admin;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.vaadin.tatu.vaadincreate.AbstractViewTest;

import com.vaadin.testbench.elements.ButtonElement;
import com.vaadin.testbench.elements.GridElement;
import com.vaadin.testbench.elements.NotificationElement;
import com.vaadin.testbench.elements.TextFieldElement;

public class CategoryManagementViewIT extends AbstractViewTest {

    @Override
    public void setup() throws Exception {
        super.setup();
        login("Admin", "admin");
        open("#!" + AdminView.VIEW_NAME + "/"
                + CategoryManagementView.VIEW_NAME);
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
        notification = $(NotificationElement.class).last();
        Assert.assertTrue(notification.getText().contains("Sports"));
        Assert.assertEquals(count, grid.getRowCount());

    }
}
