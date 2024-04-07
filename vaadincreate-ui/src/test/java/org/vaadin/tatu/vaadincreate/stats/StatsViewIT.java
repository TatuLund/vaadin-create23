package org.vaadin.tatu.vaadincreate.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openqa.selenium.By;
import org.vaadin.tatu.vaadincreate.AbstractViewTest;

public class StatsViewIT extends AbstractViewTest {

    @Override
    public void setup() throws Exception {
        super.setup();
        login("Admin", "admin");
        open("#!" + StatsView.VIEW_NAME);
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
}
