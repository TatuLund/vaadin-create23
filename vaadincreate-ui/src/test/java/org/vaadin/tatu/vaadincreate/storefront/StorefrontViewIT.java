package org.vaadin.tatu.vaadincreate.storefront;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.vaadin.tatu.vaadincreate.AbstractViewTest;

import com.deque.html.axecore.selenium.AxeBuilder;
import com.vaadin.testbench.By;
import com.vaadin.testbench.elements.ButtonElement;
import com.vaadin.testbench.elements.GridElement;
import com.vaadin.testbench.elements.TextFieldElement;
import com.vaadin.testbench.elements.UIElement;

public class StorefrontViewIT extends AbstractViewTest {

    @Override
    public void setup() {
        super.setup();
        open("#!" + StorefrontView.VIEW_NAME);
        login("Customer11", "customer11");
    }

    @After
    public void cleanup() {
        logout();
    }

    @Test
    public void navigateProductGridByKeyboardAndVerifyVisually()
            throws IOException {
        var actions = new Actions(getDriver());

        waitForElementPresent(By.id("purchase-history-grid"));
        var historyGrid = $(GridElement.class).id("purchase-history-grid");
        var productGrid = $(GridElement.class).id("purchase-grid");
        var focusedElement = focusedElement();
        assertEquals(productGrid.getCell(0, 1), focusedElement);

        actions.sendKeys(Keys.ARROW_DOWN, Keys.SPACE).perform();
        testBench().waitForVaadin();
        actions.sendKeys(Keys.ARROW_RIGHT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT)
                .perform();

        var quantityField = productGrid.getCell(1, 4).$(TextFieldElement.class)
                .first();
        focusedElement = focusedElement();
        assertEquals(quantityField, focusedElement);
        quantityField.sendKeys("4");

        historyGrid.getCell(1, 0).$(ButtonElement.class).first().click();
        testBench().waitForVaadin();

        if (visualTests()) {
            assertTrue($(UIElement.class).first()
                    .compareScreen("storefront.png"));
        }
    }

    @Test
    public void accessibility() {
        var axeBuilder = new AxeBuilder();
        // Vaadin tooltip is aria-live and is populated when hovering over
        // elements. Thus working differently than Axe assumes, but works on
        // NVDA properly.
        axeBuilder.exclude(".v-tooltip");
        // Select all checkboxes are hidden on purpose
        axeBuilder.exclude(
                "table[aria-multiselectable=\"true\"] > thead > tr > th:nth-child(1)");
        axeBuilder.exclude(
                "table[aria-rowcount=\"161\"] > thead > tr > th:nth-child(1)");

        // Ensure NumberField is displayed
        var productGrid = $(GridElement.class).id("purchase-grid");
        productGrid.getCell(0, 0).click();

        var axeResults = axeBuilder.analyze(driver);
        logViolations(axeResults);
        assertTrue(axeResults.violationFree());
    }

    private WebElement focusedElement() {
        return getDriver().switchTo().activeElement();
    }
}
