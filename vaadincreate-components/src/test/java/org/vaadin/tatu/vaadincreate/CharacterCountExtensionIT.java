package org.vaadin.tatu.vaadincreate;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

import com.vaadin.testbench.elements.TextFieldElement;

public class CharacterCountExtensionIT extends AbstractComponentTest {

    @Override
    public void setup() throws Exception {
        super.setup();
        open("#!" + CharacterCountExtensionView.NAME);
    }

    @Test
    public void characterCountExtensionWorks() {
        var countElement = getDriver()
                .findElements(By.className("v-charactercount-count")).get(0);
        var textField = $(TextFieldElement.class).first();
        Assert.assertEquals("This is a test value", textField.getValue());

        // Entering a character will wake
        textField.sendKeys("A");
        Assert.assertTrue(countElement.isDisplayed());
        Assert.assertEquals("0 / 20", countElement.getText());
        Assert.assertEquals("This is a test value", textField.getValue());

        // After clearing there is X / X characters to fill
        for (int i = 0; i < 20; i++) {
            Assert.assertEquals(i + " / 20", countElement.getText());
            textField.sendKeys(Keys.BACK_SPACE);
        }

        // Entering 10 chars and asserting
        textField.sendKeys("0123456789");
        Assert.assertEquals("10 / 20", countElement.getText());

        // Blur will hide
        textField.sendKeys(Keys.TAB);
        Assert.assertFalse(countElement.isDisplayed());
    }

}
