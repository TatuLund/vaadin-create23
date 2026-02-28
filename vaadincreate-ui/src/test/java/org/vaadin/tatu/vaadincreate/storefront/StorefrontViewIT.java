package org.vaadin.tatu.vaadincreate.storefront;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.vaadin.tatu.vaadincreate.AbstractViewTest;

import com.deque.html.axecore.selenium.AxeBuilder;
import com.vaadin.testbench.By;
import com.vaadin.testbench.elements.ButtonElement;
import com.vaadin.testbench.elements.GridElement;
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
    public void visual()
            throws IOException {
        if (visualTests()) {
            waitForElementPresent(By.id("purchase-history-grid"));
            var historyGrid = $(GridElement.class).id("purchase-history-grid");
            var productGrid = $(GridElement.class).id("purchase-grid");
            var focusedElement = focusedElement();
            assertEquals(productGrid.getCell(0, 1), focusedElement);

            productGrid.getCell(2, 1).click();
            testBench().waitForVaadin();

            historyGrid.getCell(1, 0).$(ButtonElement.class).first().click();
            testBench().waitForVaadin();

            assertTrue($(UIElement.class).first()
                    .compareScreen("storefront.png"));
        }

    }

    @Test
    public void accessibility() {
        var axeBuilder = new AxeBuilder();
        // Vaadin tooltip is aria-live and is populated when hovering over
        // elements.
        axeBuilder.exclude(".v-tooltip");
        // Select all checkbox is hidden on purpose
        axeBuilder.disableRules(List.of("empty-table-header"));

        var axeResults = axeBuilder.analyze(driver);
        logViolations(axeResults);
        assertTrue(axeResults.violationFree());
    }

    private WebElement focusedElement() {
        return getDriver().switchTo().activeElement();
    }
}
