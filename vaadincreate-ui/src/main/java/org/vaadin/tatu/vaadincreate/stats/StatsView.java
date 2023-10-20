package org.vaadin.tatu.vaadincreate.stats;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;

import com.vaadin.addon.charts.Chart;
import com.vaadin.addon.charts.model.ChartType;
import com.vaadin.addon.charts.model.DataSeries;
import com.vaadin.addon.charts.model.DataSeriesItem;
import com.vaadin.addon.charts.model.Lang;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.UIDetachedException;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
@RolesPermitted({ Role.USER, Role.ADMIN })
public class StatsView extends VerticalLayout implements View {

    public static final String VIEW_NAME = "stats";

    private StatsPresenter presenter = new StatsPresenter(this);

    private CssLayout dashboard;

    private Chart availabilityChart;

    private Chart categoryChart;

    private Chart priceChart;

    public StatsView() {
        addStyleNames(VaadinCreateTheme.STATSVIEW, ValoTheme.SCROLLABLE);
        dashboard = new CssLayout();
        dashboard.addStyleName(VaadinCreateTheme.DASHBOARD);

        var availabilityChartWrapper = new CssLayout();
        availabilityChart = new Chart(ChartType.COLUMN);
        availabilityChartWrapper
                .addStyleName(VaadinCreateTheme.DASHBOARD_CHART);
        Lang lang = new Lang();
        // Set loading label to Chart no data as loading of data is done
        // asynchronously
        lang.setNoData("Loading ...");
        var conf = availabilityChart.getConfiguration();
        conf.setTitle("Availabilities");
        conf.setLang(lang);
        availabilityChartWrapper.addComponent(availabilityChart);

        categoryChart = new Chart(ChartType.BAR);
        var categoryChartWrapper = new CssLayout();
        categoryChartWrapper
                .addStyleName(VaadinCreateTheme.DASHBOARD_CHART_WIDE);
        var cConf = categoryChart.getConfiguration();
        cConf.setTitle("Categories");
        cConf.setLang(lang);
        categoryChartWrapper.addComponent(categoryChart);

        var priceChartWrapper = new CssLayout();
        priceChart = new Chart(ChartType.PIE);
        priceChartWrapper.addStyleName(VaadinCreateTheme.DASHBOARD_CHART);
        var pConf = priceChart.getConfiguration();
        pConf.setTitle("Prices");
        pConf.setLang(lang);
        priceChartWrapper.addComponent(priceChart);

        dashboard.addComponents(availabilityChartWrapper, priceChartWrapper,
                categoryChartWrapper);
        setSizeFull();
        setMargin(false);
        addComponent(dashboard);
        setComponentAlignment(dashboard, Alignment.MIDDLE_CENTER);
    }

    public void updateStatsAsync(Map<Availability, Long> availabilityStats,
            Map<String, Long> categoryStats, Map<String, Long> priceStats) {
        try {
            getUI().access(() -> {
                var availabilitySeries = availabilitySeries(availabilityStats);
                var conf = availabilityChart.getConfiguration();
                conf.setSeries(availabilitySeries);
                var categories = (String[]) availabilitySeries.getData()
                        .stream().map(item -> item.getName())
                        .toArray(String[]::new);
                var axis = conf.getxAxis();
                axis.setCategories(categories);

                var categorySeries = categorySeries(categoryStats);
                var cConf = categoryChart.getConfiguration();
                cConf.setSeries(categorySeries);
                categoryStats.keySet()
                        .forEach(cat -> cConf.getxAxis().addCategory(cat));

                var priceSeries = priceSeries(priceStats);
                var pConf = priceChart.getConfiguration();
                pConf.setSeries(priceSeries);

                availabilityChart.drawChart();
                categoryChart.drawChart();
                priceChart.drawChart();
            });
        } catch (UIDetachedException e) {
            logger.info("Browser was closed, updates not pushed");
        }
    }

    private DataSeries categorySeries(Map<String, Long> categories) {
        var series = new DataSeries();
        categories.forEach((category, count) -> {
            var item = new DataSeriesItem(category, count);
            series.setName("Categories");
            series.add(item);
        });
        return series;
    }

    private DataSeries availabilitySeries(
            Map<Availability, Long> availabilities) {
        var series = new DataSeries();
        availabilities.forEach((availability, count) -> {
            var item = new DataSeriesItem(availability.name(), count);
            series.setName("Availabilities");
            series.add(item);
        });
        return series;
    }

    private DataSeries priceSeries(Map<String, Long> prices) {
        var series = new DataSeries();
        prices.forEach((pricebracket, count) -> {
            var item = new DataSeriesItem(pricebracket, count);
            series.setName("Prices");
            series.add(item);
        });
        return series;
    }

    @Override
    public void enter(ViewChangeEvent event) {
        // There is no need to fetch data before navigation is complete, thus we
        // trigger it in enter, not in constructor
        presenter.requestUpdateStats();
    }

    @Override
    public void detach() {
        super.detach();
        presenter.cancelUpdateStats();
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
