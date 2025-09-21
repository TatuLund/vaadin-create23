package org.vaadin.tatu.vaadincreate.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.vaadin.tatu.vaadincreate.AbstractViewTest;

import com.vaadin.testbench.elements.AbstractComponentElement;
import com.vaadin.testbench.elements.UIElement;
import com.vaadin.testbench.elementsbase.ServerClass;

public class StatsViewIT extends AbstractViewTest {

    @Override
    public void setup() {
        super.setup();
        open("#!" + StatsView.VIEW_NAME);
        login("Admin", "admin");
    }

    @After
    public void cleanup() {
        logout();
    }

    @Test
    public void chartsRendered() {
        waitForElementPresent(By.className("loaded"));

        var dashboard = findElement(By.className("dashboard"));
        var chartWrappers = dashboard.findElements(By.className("v-csslayout"));
        assertEquals(3, chartWrappers.size());
        $(ChartElement.class).all().forEach(chartWidget -> {
            var chart = chartWidget.getSvg();
            assertTrue(chart.isDisplayed());
        });
        var focused = focusedElement();
        assertEquals("availability-chart", focused.getAttribute("id"));
    }

    @Test
    public void priceChartAccessibility() {
        waitForElementPresent(By.className("loaded"));

        var chart = $(ChartElement.class).id("price-chart");
        assertEquals("0", chart.getAttribute("tabindex"));
        assertEquals("Hinnat:10 - 20 € 40,20 - 30 € 36,0 - 10 € 24",
                chart.getAttribute("aria-label"));

        // Wait until patches applied
        waitUntil(driver -> chart.findElement(By.className("highcharts-button"))
                .getAttribute("role") != null);

        // Verify the button role and tabindex, and aria-label
        var button = chart.getMenuButton();
        assertEquals("button", button.getAttribute("role"));
        assertEquals("0", button.getAttribute("tabindex"));
        assertEquals("Kaavion tulostusvalikko",
                button.getAttribute("aria-label"));

        // Verify that button opens menu with keyboard
        button.sendKeys(Keys.SPACE);
        var menu = chart.getMenuOverlay();
        assertTrue(menu.isDisplayed());
        var items = chart.getMenuItems();
        assertEquals(5, items.size());
        // Verify menu item text
        assertEquals("Tulosta kaavio", items.get(0).getText());
        assertEquals("Lataa PNG-muodossa", items.get(1).getText());
        assertEquals("Lataa JPEG-muodossa", items.get(2).getText());
        assertEquals("Lataa PDF-muodossa", items.get(3).getText());
        assertEquals("Lataa SVG-muodossa", items.get(4).getText());

        // Verify keyboard navigation
        assertEquals(focusedElement(), items.get(0));
        Actions actions = new Actions(getDriver());
        actions.sendKeys(Keys.TAB).perform();
        assertEquals(focusedElement(), items.get(1));
        actions.sendKeys(Keys.ESCAPE).perform();
        assertFalse(menu.isDisplayed());
    }

    private WebElement focusedElement() {
        return getDriver().switchTo().activeElement();
    }

    @Test
    public void categoryChartAccessibility() {
        waitForElementPresent(By.className("loaded"));

        var chart = $(ChartElement.class).id("category-chart");
        assertEquals("polite", chart.getAttribute("aria-live"));
        assertEquals("0", chart.getAttribute("tabindex"));

        // Initially all series are visible
        var series = chart.getSeries();
        assertNull(series.get(0).getAttribute("visibility"));
        assertNull(series.get(1).getAttribute("visibility"));
        assertTrue(chart.getAttribute("aria-label").contains("Määrä"));
        assertTrue(chart.getAttribute("aria-label").contains("Varastossa"));
        assertFalse(chart.getAttribute("aria-label").contains("tyhjä"));

        // Verify the button role and tabindex, and aria-label
        var buttons = chart.getLegendItemss();
        assertEquals("button", buttons.get(0).getAttribute("role"));
        assertEquals("0", buttons.get(0).getAttribute("tabindex"));
        assertTrue(buttons.get(0).getAttribute("aria-label").contains("Määrä"));
        assertEquals("button", buttons.get(1).getAttribute("role"));
        assertEquals("0", buttons.get(1).getAttribute("tabindex"));
        assertTrue(buttons.get(1).getAttribute("aria-label")
                .contains("Varastossa"));

        // Verify that added keyup event listeners are working
        buttons.get(0).sendKeys(Keys.SPACE);

        // Verify that the first series is hidden and the second is visible
        series = chart.getSeries();
        assertTrue(seriesIsHidden(series.get(0)));
        assertFalse(seriesIsHidden(series.get(1)));
        assertFalse(chart.getAttribute("aria-label").contains("Määrä"));
        assertTrue(chart.getAttribute("aria-label").contains("Varastossa"));
        assertFalse(chart.getAttribute("aria-label").contains("tyhjä"));

        // Verify that added keyup event listeners are working
        buttons.get(1).sendKeys(Keys.SPACE);

        // Verify that both series are hidden
        // and the aria-label contains "tyhjä"
        series = chart.getSeries();
        assertTrue(seriesIsHidden(series.get(0)));
        assertTrue(seriesIsHidden(series.get(1)));
        assertFalse(chart.getAttribute("aria-label").contains("Määrä"));
        assertFalse(chart.getAttribute("aria-label").contains("Varastossa"));
        assertTrue(chart.getAttribute("aria-label").contains("tyhjä"));
    }

    @Test
    public void visual() throws IOException {
        if (visualTests()) {
            waitForElementPresent(By.className("loaded"));
            // Charts have animation, wait for stabilize before compare
            driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
            assertTrue($(UIElement.class).first().compareScreen("stats.png"));
        }
    }

    private static boolean seriesIsHidden(WebElement elem) {
        if (elem.getAttribute("visibility") == null) {
            return false;
        }
        return elem.getAttribute("visibility").equals("hidden");
    }

    /**
     * TestBench element for Vaadin Charts.
     * 
     * IMHO: This should be part of TestBench elements in the framework.
     */
    @ServerClass("com.vaadin.addon.charts.Chart")
    public static class ChartElement extends AbstractComponentElement {

        /**
         * Gets the SVG element inside the chart.
         *
         * @return the SVG WebElement
         */
        public WebElement getSvg() {
            return findElement(By.tagName("svg"));
        }

        /**
         * Gets the menu button element inside the chart.
         *
         * @return the menu button WebElement, a SVG element
         */
        public WebElement getMenuButton() {
            return findElement(By.className("highcharts-button"));
        }

        /**
         * Gets the menu overlay element inside the chart.
         *
         * @return the menu overlay WebElement, a DIV element
         */
        public WebElement getMenuOverlay() {
            return findElement(By.className("highcharts-contextmenu"));
        }

        /**
         * Gets the menu item elements inside the chart menu overlay.
         *
         * @return the menu item WebElements
         */
        public List<WebElement> getMenuItems() {
            var menu = getMenuOverlay();
            return menu.findElements(By.tagName("div")).stream()
                    .filter(div -> div.getAttribute("role").equals("menuitem"))
                    .toList();
        }

        /**
         * Gets the legend item elements inside the chart.
         *
         * @return the legend item WebElements, SVG elements
         */
        public List<WebElement> getLegendItemss() {
            return findElements(By.className("highcharts-legend-item"));
        }

        /**
         * Gets the series elements inside the chart.
         *
         * @return the series WebElements, SVG elements
         */
        public List<WebElement> getSeries() {
            return findElements(By.className("highcharts-series"));
        }
    }
}
