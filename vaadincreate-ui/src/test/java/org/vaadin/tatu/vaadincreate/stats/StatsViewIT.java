package org.vaadin.tatu.vaadincreate.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.vaadin.tatu.vaadincreate.AbstractViewTest;

import com.vaadin.testbench.elements.UIElement;

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
        var dashboard = findElement(By.className("dashboard"));
        var chartWrappers = dashboard.findElements(By.className("v-csslayout"));
        assertEquals(3, chartWrappers.size());
        chartWrappers.forEach(chartWrapper -> {
            var chartWidget = chartWrapper
                    .findElement(By.className("vaadin-chart"));
            var chart = chartWidget.findElement(By.tagName("svg"));
            assertTrue(chart.isDisplayed());
        });
    }

    @Test
    public void categopryChartAccessibility() {
        waitForElementPresent(By.className("loaded"));

        var chart = findElement(By.id("category-chart"));
        assertEquals("polite", chart.getAttribute("aria-live"));
        assertEquals("0", chart.getAttribute("tabindex"));

        // Initially all series are visible
        var series = chart.findElements(By.className("highcharts-series"));
        assertNull(series.get(0).getAttribute("visibility"));
        assertNull(series.get(1).getAttribute("visibility"));
        assertTrue(chart.getAttribute("aria-label").contains("Määrä"));
        assertTrue(chart.getAttribute("aria-label").contains("Varastossa"));
        assertFalse(chart.getAttribute("aria-label").contains("tyhjä"));

        // Verify the button role and tabindex, and aria-label
        var buttons = chart
                .findElements(By.className("highcharts-legend-item"));
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
        series = chart.findElements(By.className("highcharts-series"));
        assertEquals("hidden", series.get(0).getAttribute("visibility"));
        assertNull(series.get(1).getAttribute("visibility"));
        assertFalse(chart.getAttribute("aria-label").contains("Määrä"));
        assertTrue(chart.getAttribute("aria-label").contains("Varastossa"));
        assertFalse(chart.getAttribute("aria-label").contains("tyhjä"));

        // Verify that added keyup event listeners are working
        buttons.get(1).sendKeys(Keys.SPACE);

        // Verify that both series are hidden
        // and the aria-label contains "tyhjä"
        series = chart.findElements(By.className("highcharts-series"));
        assertEquals("hidden", series.get(0).getAttribute("visibility"));
        assertEquals("hidden", series.get(1).getAttribute("visibility"));
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
}
