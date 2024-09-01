package org.vaadin.tatu.vaadincreate.stats;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.addon.charts.Chart;
import com.vaadin.addon.charts.model.ChartType;
import com.vaadin.addon.charts.model.DataSeries;
import com.vaadin.addon.charts.model.DataSeriesItem;
import com.vaadin.addon.charts.model.Lang;
import com.vaadin.addon.charts.model.YAxis;
import com.vaadin.addon.charts.model.style.SolidColor;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.UIDetachedException;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings({ "serial", "java:S2160" })
@RolesPermitted({ Role.USER, Role.ADMIN })
public class StatsView extends VerticalLayout implements View, HasI18N {

    public static final String VIEW_NAME = "stats";

    private StatsPresenter presenter = new StatsPresenter(this);

    private CssLayout dashboard;

    private Chart availabilityChart;
    private Chart categoryChart;
    private Chart priceChart;

    private Lang lang;

    private Registration resizeListener;

    public StatsView() {
        addStyleNames(VaadinCreateTheme.STATSVIEW, ValoTheme.SCROLLABLE);
        dashboard = new CssLayout();
        dashboard.addStyleNames(VaadinCreateTheme.DASHBOARD);

        lang = new Lang();
        // Set loading label to Chart no data as loading of data is done
        // asynchronously
        lang.setNoData(getTranslation(I18n.Stats.NO_DATA));

        var availabilityChartWrapper = createAvailabilityChart();

        var categoryChartWrapper = createCategoryChart();

        var priceChartWrapper = createPriceChart();

        dashboard.addComponents(availabilityChartWrapper, priceChartWrapper,
                categoryChartWrapper);
        setSizeFull();
        setMargin(false);
        addComponent(dashboard);
        setComponentAlignment(dashboard, Alignment.MIDDLE_CENTER);
    }

    private CssLayout createPriceChart() {
        var priceChartWrapper = new CssLayout();
        priceChart = new Chart(ChartType.PIE);
        priceChart.setId("price-chart");
        priceChartWrapper.addStyleName(VaadinCreateTheme.DASHBOARD_CHART);
        var conf = priceChart.getConfiguration();
        conf.setTitle(getTranslation(I18n.Stats.PRICES));
        conf.setLang(lang);
        priceChartWrapper.addComponent(priceChart);
        return priceChartWrapper;
    }

    private CssLayout createCategoryChart() {
        var categoryChartWrapper = new CssLayout();
        categoryChartWrapper
                .addStyleName(VaadinCreateTheme.DASHBOARD_CHART_WIDE);
        categoryChart = new Chart(ChartType.COLUMN);
        categoryChart.setId("category-chart");
        var conf = categoryChart.getConfiguration();
        conf.setTitle(getTranslation(I18n.CATEGORIES));
        conf.setLang(lang);
        categoryChartWrapper.addComponent(categoryChart);
        return categoryChartWrapper;
    }

    private CssLayout createAvailabilityChart() {
        var availabilityChartWrapper = new CssLayout();
        availabilityChart = new Chart(ChartType.COLUMN);
        availabilityChart.setId("availability-chart");
        availabilityChartWrapper
                .addStyleName(VaadinCreateTheme.DASHBOARD_CHART);
        var conf = availabilityChart.getConfiguration();
        conf.setTitle(getTranslation(I18n.Stats.AVAILABILITIES));
        conf.setLang(lang);
        availabilityChartWrapper.addComponent(availabilityChart);
        return availabilityChartWrapper;
    }

    /**
     * Updates the statistics asynchronously.
     * 
     * @param availabilityStats
     *            A map containing availability statistics.
     * @param categoryStats
     *            A map containing category statistics.
     * @param priceStats
     *            A map containing price statistics.
     */
    public void updateStatsAsync(Map<Availability, Long> availabilityStats,
            Map<String, Long[]> categoryStats, Map<String, Long> priceStats) {
        try {
            getUI().access(() -> {
                updateAvailabilityChart(availabilityStats);
                updateCategoryChart(categoryStats);
                updatePriceChart(priceStats);

                availabilityChart.drawChart();
                categoryChart.drawChart();
                priceChart.drawChart();

                dashboard.addStyleName("loaded");
                lang.setNoData("");
            });
        } catch (UIDetachedException e) {
            logger.info("Browser was closed, updates not pushed");
        }
    }

    // Update the charts with the new data
    private void updatePriceChart(Map<String, Long> priceStats) {
        var priceSeries = priceSeries(priceStats);
        priceSeries.setName(getTranslation(I18n.Stats.COUNT));
        var conf = priceChart.getConfiguration();
        conf.setSeries(priceSeries);
    }

    // Update the charts with the new data
    private void updateCategoryChart(Map<String, Long[]> categoryStats) {
        var conf = categoryChart.getConfiguration();

        // Show count of titles on primary axis
        var titles = categorySeries(categoryStats, 0);
        titles.setName(getTranslation(I18n.Stats.COUNT));
        conf.setSeries(titles);
        conf.getyAxis().setTitle(getTranslation(I18n.Stats.COUNT));

        // Create secondary axis for counts in stock
        // and position it to the right side
        var stockAxis = new YAxis();
        stockAxis.setOpposite(true);
        conf.addyAxis(stockAxis);
        var stockCounts = categorySeries(categoryStats, 1);
        stockCounts.setName(getTranslation(I18n.IN_STOCK));
        stockCounts.setyAxis(1);
        conf.addSeries(stockCounts);
        stockAxis.setTitle(getTranslation(I18n.IN_STOCK));
        categoryStats.keySet().forEach(cat -> conf.getxAxis().addCategory(cat));
    }

    // Update the charts with the new data
    private void updateAvailabilityChart(
            Map<Availability, Long> availabilityStats) {
        var availabilitySeries = availabilitySeries(availabilityStats);
        availabilitySeries.setName(getTranslation(I18n.Stats.COUNT));
        var conf = availabilityChart.getConfiguration();
        conf.setSeries(availabilitySeries);
        conf.getLegend().setEnabled(false);
        var categories = availabilitySeries.getData().stream()
                .map(item -> item.getName()).toArray(String[]::new);
        var axis = conf.getxAxis();
        axis.setCategories(categories);
    }

    private DataSeries categorySeries(Map<String, Long[]> categories,
            int index) {
        var series = new DataSeries();
        categories.forEach((category, count) -> {
            var item = new DataSeriesItem(category, count[index]);
            series.setName(getTranslation(I18n.CATEGORIES));
            series.add(item);
        });
        return series;
    }

    private DataSeries availabilitySeries(
            Map<Availability, Long> availabilities) {
        var series = new DataSeries();
        availabilities.forEach((availability, count) -> {
            var item = new DataSeriesItem(availability.name(), count);
            item.setColor(toColor(availability));
            series.setName(getTranslation(I18n.Stats.AVAILABILITIES));
            series.add(item);
        });
        return series;
    }

    private static SolidColor toColor(Availability availability) {
        String color;
        switch (availability) {
        case COMING:
            color = VaadinCreateTheme.COLOR_COMING;
            break;
        case AVAILABLE:
            color = VaadinCreateTheme.COLOR_AVAILABLE;
            break;
        default:
            color = VaadinCreateTheme.COLOR_DISCONTINUED;
            break;
        }
        return new SolidColor(color);
    }

    private DataSeries priceSeries(Map<String, Long> prices) {
        var series = new DataSeries();
        prices.forEach((pricebracket, count) -> {
            var item = new DataSeriesItem(pricebracket, count);
            series.setName(getTranslation(I18n.Stats.PRICES));
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
    public void attach() {
        super.attach();
        // Vaadin responsive forces layout only when break point changes,
        // however Chart requires re-layout also when window size changes
        // when using non-fixed sizes.
        resizeListener = getUI().getPage().addBrowserWindowResizeListener(
                e -> JavaScript.eval("vaadin.forceLayout()"));
    }

    @Override
    public void detach() {
        super.detach();
        resizeListener.remove();
        presenter.cancelUpdateStats();
    }

    public void setLoading() {
        dashboard.removeStyleName("loaded");
    }

    private static Logger logger = LoggerFactory.getLogger(StatsView.class);

}
