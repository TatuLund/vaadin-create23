package org.vaadin.tatu.vaadincreate.stats;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.ChartAccessibilityExtension;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.VaadinCreateView;
import org.vaadin.tatu.vaadincreate.auth.RolesPermitted;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.User.Role;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.addon.charts.Chart;
import com.vaadin.addon.charts.model.Buttons;
import com.vaadin.addon.charts.model.ChartType;
import com.vaadin.addon.charts.model.Configuration;
import com.vaadin.addon.charts.model.DataSeries;
import com.vaadin.addon.charts.model.DataSeriesItem;
import com.vaadin.addon.charts.model.Exporting;
import com.vaadin.addon.charts.model.Lang;
import com.vaadin.addon.charts.model.YAxis;
import com.vaadin.addon.charts.model.style.SolidColor;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
@RolesPermitted({ Role.USER, Role.ADMIN })
public class StatsView extends VerticalLayout implements VaadinCreateView {

    private static final Logger logger = LoggerFactory
            .getLogger(StatsView.class);

    public static final String VIEW_NAME = "stats";

    private StatsPresenter presenter = new StatsPresenter(this);

    private CssLayout dashboard;

    private CustomChart availabilityChart = new CustomChart(ChartType.COLUMN);
    private CustomChart categoryChart = new CustomChart(ChartType.COLUMN);
    private CustomChart priceChart = new CustomChart(ChartType.PIE);

    private Lang lang;

    @Nullable
    private Registration resizeListener;

    @Nullable
    private UI ui;

    public StatsView() {
        addStyleNames(VaadinCreateTheme.STATSVIEW, ValoTheme.SCROLLABLE);
        dashboard = new CssLayout();
        dashboard.addStyleNames(VaadinCreateTheme.DASHBOARD);

        lang = new Lang();
        // Set loading label to Chart no data as loading of data is done
        // asynchronously
        lang.setNoData(getTranslation(I18n.Stats.NO_DATA));

        var availabilityChartWrapper = configureAvailabilityChart();

        var categoryChartWrapper = configureCategoryChart();

        var priceChartWrapper = configurePriceChart();

        dashboard.addComponents(availabilityChartWrapper, priceChartWrapper,
                categoryChartWrapper);
        setSizeFull();
        setMargin(false);
        addComponent(dashboard);
        // Create the export configuration
        var exporting = new Exporting(true);
        // Customize the file name of the download file
        exporting.setFilename("chart");
        // Use the exporting configuration in the chart, note in the
        // real production environment the URL should point to a
        // actual address where the application is hosted instead
        // of localhost.
        exporting.setUrl("http://charts:export@127.0.0.1:8083/");
        exporting.setButtons(new Buttons());
        categoryChart.getConfiguration().setExporting(exporting);
        availabilityChart.getConfiguration().setExporting(exporting);
        priceChart.getConfiguration().setExporting(exporting);
        setComponentAlignment(dashboard, Alignment.MIDDLE_CENTER);
    }

    private CssLayout configurePriceChart() {
        var priceChartWrapper = new CssLayout();
        priceChart.setId("price-chart");
        priceChart.addStyleName(VaadinCreateTheme.DASHBOARD_CHART_FOCUSRING);
        priceChartWrapper.addStyleName(VaadinCreateTheme.DASHBOARD_CHART);
        var conf = priceChart.getConfiguration();
        conf.setTitle(getTranslation(I18n.Stats.PRICES));
        conf.setLang(lang);
        configureTooltip(conf);
        priceChartWrapper.addComponent(priceChart);
        return priceChartWrapper;
    }

    private CssLayout configureCategoryChart() {
        var categoryChartWrapper = new CssLayout();
        categoryChartWrapper
                .addStyleName(VaadinCreateTheme.DASHBOARD_CHART_WIDE);
        categoryChart.setId("category-chart");
        categoryChart.addStyleName(VaadinCreateTheme.DASHBOARD_CHART_FOCUSRING);
        var conf = categoryChart.getConfiguration();
        conf.setTitle(getTranslation(I18n.CATEGORIES));
        conf.setLang(lang);
        configureTooltip(conf);
        categoryChartWrapper.addComponent(categoryChart);
        categoryChart.addLegendItemClickListener(legendItemClicked -> {
            var series = (DataSeries) legendItemClicked.getSeries();
            series.setVisible(!series.isVisible(), true);
            var titles = (DataSeries) categoryChart.getConfiguration()
                    .getSeries().get(0);
            var stockCounts = (DataSeries) categoryChart.getConfiguration()
                    .getSeries().get(1);
            categoryChart.setAttribute(AriaAttributes.LIVE, "polite");
            updateCategoryChartAccessibilityAttributes(titles, stockCounts);
        });
        return categoryChartWrapper;
    }

    private CssLayout configureAvailabilityChart() {
        var availabilityChartWrapper = new CssLayout();
        availabilityChart
                .addStyleName(VaadinCreateTheme.DASHBOARD_CHART_FOCUSRING);
        availabilityChart.setId("availability-chart");
        availabilityChartWrapper
                .addStyleName(VaadinCreateTheme.DASHBOARD_CHART);
        var conf = availabilityChart.getConfiguration();
        conf.setTitle(getTranslation(I18n.Stats.AVAILABILITIES));
        conf.setLang(lang);
        configureTooltip(conf);
        availabilityChartWrapper.addComponent(availabilityChart);
        return availabilityChartWrapper;
    }

    private static void configureTooltip(Configuration conf) {
        assert conf != null : "Configuration must not be null";
        conf.getTooltip()
                .setBackgroundColor(new SolidColor("rgba(50,50,50,0.9)"));
        conf.getTooltip().getStyle().setColor(new SolidColor("white"));
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
        Utils.access(ui, () -> {
            if (isAttached()) {
                updateAvailabilityChart(availabilityStats);
                updateCategoryChart(categoryStats);
                updatePriceChart(priceStats);

                availabilityChart.drawChart();
                categoryChart.drawChart();
                priceChart.drawChart();

                dashboard.addStyleName("loaded");
                lang.setNoData("");
                logger.debug("Stats updated");
            }
        });
    }

    // Update the charts with the new data
    private void updatePriceChart(Map<String, Long> priceStats) {
        assert priceStats != null : "Price stats must not be null";

        var priceSeries = priceSeries(priceStats);
        priceSeries.setName(getTranslation(I18n.Stats.COUNT));
        var conf = priceChart.getConfiguration();
        conf.setSeries(priceSeries);

        updatePriceChartAccessibilityAttributes(priceSeries);
    }

    @SuppressWarnings("java:S1192")
    private void updatePriceChartAccessibilityAttributes(
            DataSeries priceSeries) {
        var alt = String.format("%s:%s", getTranslation(I18n.Stats.PRICES),
                priceSeries
                        .getData().stream().map(data -> String.format("%s %s",
                                data.getName(), data.getY()))
                        .collect(Collectors.joining(",")));
        priceChart.setAriaLabel(alt);
    }

    // Update the charts with the new data
    private void updateCategoryChart(Map<String, Long[]> categoryStats) {
        assert categoryStats != null : "Category stats must not be null";
        var conf = categoryChart.getConfiguration();

        // Show count of titles on primary axis
        conf.removexAxes();
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

        updateCategoryChartAccessibilityAttributes(titles, stockCounts);
    }

    @SuppressWarnings("java:S5411")
    private void updateCategoryChartAccessibilityAttributes(DataSeries titles,
            DataSeries stockCounts) {
        var alt1 = titles.isVisible() ? String.format("%s %s:%s",
                getTranslation(I18n.Stats.CATEGORIES),
                getTranslation(I18n.Stats.COUNT),
                titles.getData().stream()
                        .map(data -> String.format("%s %s", data.getName(),
                                data.getY()))
                        .collect(Collectors.joining(",")))
                : "";
        var alt2 = stockCounts.isVisible() ? String.format("%s %s:%s",
                getTranslation(I18n.Stats.CATEGORIES),
                getTranslation(I18n.IN_STOCK),
                stockCounts.getData().stream()
                        .map(data -> String.format("%s %s", data.getName(),
                                data.getY()))
                        .collect(Collectors.joining(",")))
                : "";
        if (alt1.isEmpty() && alt2.isEmpty()) {
            alt1 = getTranslation(I18n.Stats.EMPTY);
        }
        categoryChart.setAriaLabel(String.format("%s %s", alt1, alt2));
    }

    // Update the charts with the new data
    private void updateAvailabilityChart(
            Map<Availability, Long> availabilityStats) {
        assert availabilityStats != null : "Availability stats must not be null";

        var availabilitySeries = availabilitySeries(availabilityStats);
        availabilitySeries.setName(getTranslation(I18n.Stats.COUNT));
        var conf = availabilityChart.getConfiguration();
        conf.setSeries(availabilitySeries);
        conf.getLegend().setEnabled(false);
        var categories = availabilitySeries.getData().stream()
                .map(item -> item.getName()).toArray(String[]::new);
        var xaxis = conf.getxAxis();
        xaxis.setCategories(categories);
        var yaxis = conf.getyAxis();
        yaxis.setTitle(getTranslation(I18n.Stats.COUNT));

        updateAvailabilityChartAccessibilityAttributes(availabilitySeries);
    }

    private void updateAvailabilityChartAccessibilityAttributes(
            DataSeries availabilitySeries) {
        var alt = String.format("%s:%s",
                getTranslation(I18n.Stats.AVAILABILITIES),
                availabilitySeries
                        .getData().stream().map(data -> String.format("%s %s",
                                data.getName(), data.getY()))
                        .collect(Collectors.joining(",")));
        availabilityChart.setAriaLabel(alt);
    }

    private DataSeries categorySeries(Map<String, Long[]> categories,
            int index) {
        assert categories != null : "Categories must not be null";

        var series = new DataSeries();
        series.setName(getTranslation(I18n.CATEGORIES));
        categories.forEach((category, count) -> {
            var item = new DataSeriesItem(category, count[index]);
            series.add(item);
        });
        return series;
    }

    private DataSeries availabilitySeries(
            Map<Availability, Long> availabilities) {
        assert availabilities != null : "Availabilities must not be null";

        var series = new DataSeries();
        availabilities.forEach((availability, count) -> {
            var item = new DataSeriesItem(availability.name(), count);
            item.setColor(toColor(availability));
            series.add(item);
        });
        series.setName(getTranslation(I18n.Stats.AVAILABILITIES));
        return series;
    }

    private static SolidColor toColor(Availability availability) {
        assert availability != null : "Availability must not be null";

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
        assert prices != null : "Prices must not be null";

        var series = new DataSeries();
        prices.forEach((pricebracket, count) -> {
            var item = new DataSeriesItem(pricebracket, count);
            series.add(item);
        });
        series.setName(getTranslation(I18n.Stats.PRICES));
        return series;
    }

    @Override
    public void enter(ViewChangeEvent viewChange) {
        openingView(VIEW_NAME);
        // There is no need to fetch data before navigation is complete, thus we
        // trigger it in enter, not in constructor
        presenter.requestUpdateStats();
    }

    @Override
    public void attach() {
        super.attach();
        ui = getUI();
        // Vaadin responsive forces layout only when break point changes,
        // however Chart requires re-layout also when window size changes
        // when using non-fixed sizes.
        resizeListener = ui.getPage().addBrowserWindowResizeListener(
                resizeEvent -> JavaScript.eval("vaadin.forceLayout()"));
        availabilityChart.focus();
    }

    @Override
    public void detach() {
        super.detach();
        resizeListener.remove();
        presenter.cancelUpdateStats();
        ui = null;
    }

    public void setLoadingAsync() {
        Utils.access(ui, () -> {
            dashboard.removeStyleName("loaded");
            categoryChart.getConfiguration().removeyAxes();
        });
    }

    /**
     * CustomChart is an extension of the Chart class that allows for setting
     * custom attributes. It uses the AttributeExtension to manage these
     * attributes. Furthermore, it implements Focusable to allow focus
     * management and HasI18N for internationalization support. Also, it
     * integrates ChartAccessibilityExtension to enhance accessibility features
     * for the chart.
     */
    public static class CustomChart extends Chart
            implements HasAttributes<CustomChart>, Focusable, HasI18N {

        @Nullable
        ChartAccessibilityExtension a11y;
        private int tabIndex = -1;

        public CustomChart(ChartType type) {
            super(type);
            a11y = ChartAccessibilityExtension.of(this);
            a11y.setLegendsClickable(
                    getTranslation(I18n.Stats.LEGEND_CLICKABLE));
            a11y.setContextMenu(getTranslation(I18n.Stats.CONTEXT_MENU));
            a11y.setMenuEntries(Arrays.asList(
                    getTranslation(I18n.Stats.MENU_ENTRIES).split(",")));
            setTabIndex(0);
            setRole(AriaRoles.FIGURE);
        }

        @Override
        public void drawChart() {
            super.drawChart();
            if (a11y != null) {
                a11y.applyPatches();
            }
        }

        @Override
        public int getTabIndex() {
            return tabIndex;
        }

        @Override
        public void setTabIndex(int tabIndex) {
            setAttribute("tabindex", tabIndex);
            this.tabIndex = tabIndex;
        }

        @Override
        public void focus() {
            super.focus();
            assert getId() != null : "Chart must have an id set to be focused";
            JavaScript.eval("""
                    setTimeout(() => {
                        var chart = document.getElementById("%s");
                        if (chart) { chart.focus(); }
                    }, 100);
                    """.formatted(getId()));
        }
    }

}
