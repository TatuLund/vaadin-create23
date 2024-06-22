package org.vaadin.tatu.vaadincreate.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AbstractUITest;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateUI;

import com.vaadin.addon.charts.Chart;
import com.vaadin.addon.charts.model.DataSeries;
import com.vaadin.server.ServiceException;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.VerticalLayout;

public class StatsViewTest extends AbstractUITest {

    private StatsView view;
    private VaadinCreateUI ui;
    private Map<String, Number> values;

    @Before
    public void setup() throws ServiceException {
        ui = new VaadinCreateUI();
        mockVaadin(ui);
        login();

        view = navigate(StatsView.VIEW_NAME, StatsView.class);

        var dashboard = $(CssLayout.class)
                .styleName(VaadinCreateTheme.DASHBOARD).first();
        waitForCharts(dashboard);
    }

    @Test
    public void priceStats() {
        values = new HashMap<>();
        values.put("10 - 20 €", 40);
        values.put("20 - 30 €", 36);
        values.put("0 - 10 €", 24);

        var priceChart = $(Chart.class).id("price-chart");
        var series = (DataSeries) priceChart.getConfiguration().getSeries()
                .get(0);
        series.getData().forEach(item -> {
            var name = item.getName();
            var y = item.getY();
            assertEquals(values.get(name), y.intValue());
        });
    }

    @Test
    public void availabilityStats() {
        values = new HashMap<>();
        values.put("DISCONTINUED", 34);
        values.put("COMING", 38);
        values.put("AVAILABLE", 28);

        var priceChart = $(Chart.class).id("availability-chart");
        var series = (DataSeries) priceChart.getConfiguration().getSeries()
                .get(0);
        series.getData().forEach(item -> {
            var name = item.getName();
            var y = item.getY();
            assertEquals(values.get(name), y.intValue());
        });
    }

    @Test
    public void categoryStats() {
        values = new HashMap<>();
        values.put("Non-fiction", 15);
        values.put("Sci-fi", 26);
        values.put("Thriller", 14);
        values.put("Romance", 23);
        values.put("Mystery", 16);
        values.put("Cookbooks", 7);
        values.put("Children's books", 26);
        values.put("Best sellers", 22);

        var priceChart = $(Chart.class).id("category-chart");
        var series = (DataSeries) priceChart.getConfiguration().getSeries()
                .get(0);
        series.getData().forEach(item -> {
            var name = item.getName();
            var y = item.getY();
            assertEquals(values.get(name), y.intValue());
        });

        values = new HashMap<>();
        values.put("Non-fiction", 622);
        values.put("Sci-fi", 2321);
        values.put("Thriller", 863);
        values.put("Romance", 1014);
        values.put("Mystery", 665);
        values.put("Cookbooks", 546);
        values.put("Children's books", 2082);
        values.put("Best sellers", 1267);

        series = (DataSeries) priceChart.getConfiguration().getSeries().get(1);
        series.getData().forEach(item -> {
            var name = item.getName();
            var y = item.getY();
            assertEquals(values.get(name), y.intValue());
        });
    }

    @After
    public void cleanUp() {
        logout();
        tearDown();
    }
}
