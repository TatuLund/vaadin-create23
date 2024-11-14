package org.vaadin.tatu.vaadincreate;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.testbench.elements.ButtonElement;
import com.vaadin.testbench.elements.LabelElement;
import com.vaadin.testbench.elements.MenuBarElement;
import com.vaadin.testbench.elements.TextAreaElement;

public class AboutViewIT extends AbstractViewTest {

    @Override
    public void setup() throws Exception {
        super.setup();
        open("#!" + AboutView.VIEW_NAME);
        login("Admin", "admin");
    }

    @After
    public void cleanup() {
        $(MenuBarElement.class).first().findElement(By.id("logout-2")).click();
    }

    @Test
    public void leaveMessageSanitized() {
        waitForElementPresent(By.id("admins-note"));
        var note = $(LabelElement.class).id("admins-note");
        var oldText = note.getHTML();

        $(ButtonElement.class).id("admin-edit").click();
        var area = $(TextAreaElement.class).id("admins-text-area");
        // If not sanitized this would be XSS
        area.setValue(
                "<b><img src=1 onerror=alert(document.domain)>A new message</b>");
        // Changing field value should hide it
        waitForElementNotPresent(By.id("admins-text-area"));
        // Assert the new value
        waitForElementPresent(By.id("admins-note"));
        note = $(LabelElement.class).id("admins-note");
        Assert.assertEquals("<b><img>A new message</b>", note.getHTML());

        // Return the old value
        $(ButtonElement.class).id("admin-edit").click();
        waitForElementPresent(By.id("admins-text-area"));
        area = $(TextAreaElement.class).id("admins-text-area");
        area.setValue(oldText);
        waitForElementNotPresent(By.id("admins-text-area"));
    }
}
