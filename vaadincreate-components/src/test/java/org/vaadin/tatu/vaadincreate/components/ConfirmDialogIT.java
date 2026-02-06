package org.vaadin.tatu.vaadincreate.components;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.testbench.elements.ButtonElement;
import com.vaadin.testbench.elements.NotificationElement;

@SuppressWarnings("null")
public class ConfirmDialogIT extends AbstractComponentTest {

    @Override
    public void setup() throws Exception {
        super.setup();
        open("#!" + ConfirmDialogView.NAME);
    }

    @Test
    public void openAndCloseByEscapeAndEnter() {
        var button = $(ButtonElement.class).first();
        button.click();
        var actions = new Actions(getDriver());
        actions.sendKeys(Keys.ESCAPE).perform();
        var notification = $(NotificationElement.class).last();
        assertEquals("Cancelled", notification.getCaption());

        button.click();
        new Actions(getDriver()).sendKeys(Keys.ENTER).perform();
        notification = $(NotificationElement.class).last();
        assertEquals("Confirmed", notification.getCaption());
    }
}
