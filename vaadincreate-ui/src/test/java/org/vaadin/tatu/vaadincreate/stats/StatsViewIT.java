package org.vaadin.tatu.vaadincreate.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.vaadin.tatu.vaadincreate.AbstractViewTest;

import com.deque.html.axecore.selenium.AxeBuilder;
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
    public void availabilityChartContent() {
        waitForElementPresent(By.className("loaded"));

        var chart = $(ChartElement.class).id("availability-chart");
        assertEquals("Saatavuustiedot", chart.getTitle());

        var yAxisTitles = chart.getYAxisTitles();
        assertEquals(1, yAxisTitles.size());
        assertEquals("Määrä", yAxisTitles.get(0));

        var yAxisLabels = chart.getYAxisLabels(0);
        assertEquals("0", yAxisLabels.get(0));
        assertEquals("40", yAxisLabels.get(4));

        var xAxisLabels = chart.getXAxisLabels();
        assertEquals(3, xAxisLabels.size());

        assertEquals("COMING", xAxisLabels.get(0));
        assertEquals("AVAILABLE", xAxisLabels.get(1));
        assertEquals("DISCONTINUED", xAxisLabels.get(2));

        var focused = focusedElement();
        assertEquals("availability-chart", focused.getAttribute("id"));
    }

    @Test
    public void priceChartContent() {
        waitForElementPresent(By.className("loaded"));
        // Wait for chart animation to stabilize
        wait(Duration.ofSeconds(1));

        var chart = $(ChartElement.class).id("price-chart");

        assertEquals("Hinnat", chart.getTitle());

        // Verify data labels
        var dataLabels = chart.getDataLabels();
        assertEquals(3, dataLabels.size());
        assertTrue(dataLabels.contains("0 - 10 €"));
        assertTrue(dataLabels.contains("10 - 20 €"));
        assertTrue(dataLabels.contains("20 - 30 €"));
    }

    @Test
    public void categoryChartContent() {
        waitForElementPresent(By.className("loaded"));

        var chart = $(ChartElement.class).id("category-chart");

        assertEquals("Kategoriat", chart.getTitle());

        // Verify Y axes
        var yAxisTitles = chart.getYAxisTitles();
        assertEquals(2, yAxisTitles.size());
        assertEquals("Määrä", yAxisTitles.get(0));
        assertEquals("Varastossa", yAxisTitles.get(1));

        var yAxis0Labels = chart.getYAxisLabels(0);
        assertEquals("0", yAxis0Labels.get(0));
        assertEquals("40", yAxis0Labels.get(4));

        var yAxis1Labels = chart.getYAxisLabels(1);
        assertEquals("0", yAxis1Labels.get(0));
        assertEquals("4k", yAxis1Labels.get(4));

        // Verify X axis
        var xAxisLabels = chart.getXAxisLabels();
        assertEquals(8, xAxisLabels.size());
        assertEquals("Non-fiction", xAxisLabels.get(0));
        assertEquals("Sci-fi", xAxisLabels.get(1));
        assertEquals("Thriller", xAxisLabels.get(2));
        assertEquals("Romance", xAxisLabels.get(3));
        assertEquals("Mystery", xAxisLabels.get(4));
        assertEquals("Cookbooks", xAxisLabels.get(5));
        assertEquals("Children's books", xAxisLabels.get(6));
        assertEquals("Best sellers", xAxisLabels.get(7));
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
        assertNull(chart.getAttribute("aria-live"));
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
        assertEquals("polite", chart.getAttribute("aria-live"));

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
            wait(Duration.ofSeconds(1));
            assertTrue($(UIElement.class).first().compareScreen("stats.png"));
        }
    }

    @Test
    public void accessibility() {
        waitForElementPresent(By.className("loaded"));

        var chart = $(ChartElement.class).id("price-chart");
        chart.getMenuButton().click();

        var axeBuilder = new AxeBuilder();
        axeBuilder.exclude(".v-tooltip");

        var axeResults = axeBuilder.analyze(driver);
        logViolations(axeResults);
        assertTrue(axeResults.violationFree());
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
            var menu = getMenuOverlay().findElement(By.tagName("div"));
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return menu.findElements(By.tagName("div"));
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

        /**
         * Gets the Y axis title texts inside the chart.
         *
         * @return the list of Y axis title texts
         */
        public List<String> getYAxisTitles() {
            return findElements(By.className("highcharts-yaxis-title")).stream()
                    .map(WebElement::getText).toList();
        }

        /**
         * Gets the Y axis label texts inside the chart.
         *
         * @param index
         *            the Y axis index, 0 = first
         * @return the list of Y axis label texts
         */
        public List<String> getYAxisLabels(int index) {
            var yAxes = findElements(By.className("highcharts-yaxis-labels"));
            var labelsContainer = yAxes.get(index);
            return labelsContainer.findElements(By.tagName("text")).stream()
                    .map(ChartElement::getLabelText).toList();
        }

        /**
         * Gets the X axis label texts inside the chart.
         *
         * @return the list of X axis label texts
         */
        public List<String> getXAxisLabels() {
            var labelsContainer = findElement(
                    By.className("highcharts-xaxis-labels"));
            var texts = labelsContainer.findElements(By.tagName("text"));
            return texts.stream().map(ChartElement::getLabelText).toList();
        }

        private static String getLabelText(WebElement text) {
            return text.findElements(By.tagName("tspan")).stream()
                    .map(WebElement::getText).collect(Collectors.joining(" "));
        }

        /**
         * Gets the data label texts inside the chart.
         *
         * @return the list of data label texts
         */
        public List<String> getDataLabels() {
            return findElements(By.className("highcharts-data-labels")).stream()
                    .flatMap(
                            dl -> dl.findElements(By.tagName("tspan")).stream())
                    .map(WebElement::getText).toList();
        }

        /**
         * Gets the chart title text.
         *
         * @return the chart title text
         */
        public String getTitle() {
            var title = findElement(By.className("highcharts-title"));
            return getLabelText(title);
        }

        /**
         * Gets the chart subtitle text.
         *
         * @return the chart subtitle text
         */
        public String getSubTitle() {
            var subtitle = findElement(By.className("highcharts-subtitle"));
            return getLabelText(subtitle);
        }
    }
}
