package org.vaadin.tatu.vaadincreate;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

import com.vaadin.testbench.elements.LabelElement;
import com.vaadin.testbench.elements.TextFieldElement;

public class ResetButtonForTextFieldIT extends AbstractComponentTest {

    @Override
    public void setup() throws Exception {
        super.setup();
        open("#!" + ResetButtonForTextFieldView.NAME);
    }

    @Test
    public void resetButtonFoundAndClearsWhenClicked() {
        var field = $(TextFieldElement.class).first();
        Assert.assertTrue(field.getClassNames()
                .contains("resetbuttonfortextfield-textfield"));
        var button = driver.findElement(
                By.className("resetbuttonfortextfield-resetbutton"));
        Assert.assertTrue(button.isDisplayed());
        button.click();
        Assert.assertEquals("", field.getValue());
        var value = $(LabelElement.class).first();
        Assert.assertEquals("Value:", value.getText());
        Assert.assertFalse(button.isDisplayed());
    }

    @Test
    public void resetButtonFoundAndClearsKeyboard() {
        var field = $(TextFieldElement.class).first();
        Assert.assertTrue(field.getClassNames()
                .contains("resetbuttonfortextfield-textfield"));
        var button = driver.findElement(
                By.className("resetbuttonfortextfield-resetbutton"));
        Assert.assertTrue(button.isDisplayed());

        field.focus();
        field.sendKeys(Keys.TAB, Keys.ENTER);

        Assert.assertEquals("", field.getValue());
        var value = $(LabelElement.class).first();
        Assert.assertEquals("Value:", value.getText());
        Assert.assertFalse(button.isDisplayed());
    }

}
