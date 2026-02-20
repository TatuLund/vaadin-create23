package org.vaadin.tatu.vaadincreate.purchases;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService.MonthlyPurchaseStat;
import org.vaadin.tatu.vaadincreate.backend.PurchaseService.ProductPurchaseStat;
import org.vaadin.tatu.vaadincreate.common.CustomChart;
import org.vaadin.tatu.vaadincreate.common.TabView;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.purchases.PurchasesStatsPresenter.PurchaseStatistics;
import org.vaadin.tatu.vaadincreate.stats.StatsView;
import org.vaadin.tatu.vaadincreate.util.Utils;

import com.vaadin.addon.charts.model.ChartType;
import com.vaadin.addon.charts.model.Configuration;
import com.vaadin.addon.charts.model.DataSeries;
import com.vaadin.addon.charts.model.DataSeriesItem;
import com.vaadin.addon.charts.model.Lang;
import com.vaadin.addon.charts.model.XAxis;
import com.vaadin.addon.charts.model.YAxis;
import com.vaadin.addon.charts.model.style.SolidColor;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * Stats tab for {@link PurchasesView}. Displays three charts based solely on
 * {@code COMPLETED} purchases:
 * <ul>
 * <li>Pie chart – top 10 most purchased products by quantity
 * ({@code purchases-top-products-chart})</li>
 * <li>Pie chart – top 10 least purchased products by quantity
 * ({@code purchases-least-products-chart})</li>
 * <li>Line chart – completed purchase amount per month over the last 12 months
 * ({@code purchases-per-month-chart})</li>
 * </ul>
 *
 * <p>
 * Chart data is loaded asynchronously via {@link PurchasesStatsPresenter} and
 * pushed to the UI through {@link Utils#access(UI, Runnable)}, following the
 * same pattern as {@link StatsView}.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class PurchasesStatsTab extends VerticalLayout implements TabView {

    private static final Logger logger = LoggerFactory
            .getLogger(PurchasesStatsTab.class);

    public static final String VIEW_NAME = StatsView.VIEW_NAME;

    private final PurchasesStatsPresenter presenter;
    private final CssLayout dashboard;
    private final Lang lang;

    private final CustomChart topProductsChart = new CustomChart(
            ChartType.PIE);
    private final CustomChart leastProductsChart = new CustomChart(
            ChartType.PIE);
    private final CustomChart monthlyChart = new CustomChart(ChartType.LINE);

    @Nullable
    private UI ui;

    /**
     * Constructs the Stats tab and wires up all three charts.
     */
    public PurchasesStatsTab() {
        setSizeFull();
        presenter = new PurchasesStatsPresenter(this);

        lang = new Lang();
        lang.setNoData(getTranslation(I18n.Stats.NO_DATA));

        dashboard = new CssLayout();
        dashboard.addStyleName(VaadinCreateTheme.DASHBOARD);

        dashboard.addComponents(
                configureTopProductsChart(),
                configureLeastProductsChart(),
                configureMonthlyChart());

        setMargin(false);
        addComponent(dashboard);
        setComponentAlignment(dashboard, Alignment.MIDDLE_CENTER);
    }

    // ---- chart configuration -----------------------------------------------

    private CssLayout configureTopProductsChart() {
        var wrapper = new CssLayout();
        wrapper.addStyleName(VaadinCreateTheme.DASHBOARD_CHART);
        topProductsChart.setId("purchases-top-products-chart");
        topProductsChart
                .addStyleName(VaadinCreateTheme.DASHBOARD_CHART_FOCUSRING);
        var conf = topProductsChart.getConfiguration();
        conf.setTitle(getTranslation(I18n.Storefront.TOP_PRODUCTS));
        conf.setLang(lang);
        configureTooltip(conf);
        wrapper.addComponent(topProductsChart);
        return wrapper;
    }

    private CssLayout configureLeastProductsChart() {
        var wrapper = new CssLayout();
        wrapper.addStyleName(VaadinCreateTheme.DASHBOARD_CHART);
        leastProductsChart.setId("purchases-least-products-chart");
        leastProductsChart
                .addStyleName(VaadinCreateTheme.DASHBOARD_CHART_FOCUSRING);
        var conf = leastProductsChart.getConfiguration();
        conf.setTitle(getTranslation(I18n.Storefront.LEAST_PRODUCTS));
        conf.setLang(lang);
        configureTooltip(conf);
        wrapper.addComponent(leastProductsChart);
        return wrapper;
    }

    private CssLayout configureMonthlyChart() {
        var wrapper = new CssLayout();
        wrapper.addStyleName(VaadinCreateTheme.DASHBOARD_CHART_WIDE);
        monthlyChart.setId("purchases-per-month-chart");
        monthlyChart.addStyleName(VaadinCreateTheme.DASHBOARD_CHART_FOCUSRING);
        var conf = monthlyChart.getConfiguration();
        conf.setTitle(getTranslation(I18n.Storefront.MONTHLY_TOTALS));
        conf.setLang(lang);
        configureTooltip(conf);
        wrapper.addComponent(monthlyChart);
        return wrapper;
    }

    private static void configureTooltip(Configuration conf) {
        assert conf != null : "Configuration must not be null";
        conf.getTooltip()
                .setBackgroundColor(new SolidColor("rgba(50,50,50,0.9)"));
        conf.getTooltip().getStyle().setColor(new SolidColor("white"));
    }

    // ---- async update -------------------------------------------------------

    /**
     * Receives computed statistics on (possibly) a background thread and
     * schedules a safe UI update.
     *
     * @param stats
     *            the computed statistics, must not be null
     */
    public void updateStatsAsync(PurchaseStatistics stats) {
        Utils.access(ui, () -> {
            if (isAttached()) {
                updateTopProductsChart(stats.topProducts());
                updateLeastProductsChart(stats.leastProducts());
                updateMonthlyChart(stats.monthlyTotals());

                topProductsChart.drawChart();
                leastProductsChart.drawChart();
                monthlyChart.drawChart();

                dashboard.addStyleName("loaded");
                lang.setNoData("");
                logger.debug("Purchase statistics updated");
            }
        });
    }

    /**
     * Marks the dashboard as loading (not yet ready).
     */
    public void setLoadingAsync() {
        Utils.access(ui, () -> dashboard.removeStyleName("loaded"));
    }

    // ---- chart data update helpers ------------------------------------------

    private void updateTopProductsChart(List<ProductPurchaseStat> stats) {
        var series = buildProductSeries(stats,
                getTranslation(I18n.Storefront.TOP_PRODUCTS));
        topProductsChart.getConfiguration().setSeries(series);
        updatePieChartAriaLabel(topProductsChart,
                getTranslation(I18n.Storefront.TOP_PRODUCTS), series);
    }

    private void updateLeastProductsChart(List<ProductPurchaseStat> stats) {
        var series = buildProductSeries(stats,
                getTranslation(I18n.Storefront.LEAST_PRODUCTS));
        leastProductsChart.getConfiguration().setSeries(series);
        updatePieChartAriaLabel(leastProductsChart,
                getTranslation(I18n.Storefront.LEAST_PRODUCTS), series);
    }

    private static DataSeries buildProductSeries(
            List<ProductPurchaseStat> stats, String seriesName) {
        var series = new DataSeries();
        series.setName(seriesName);
        stats.forEach(stat -> series
                .add(new DataSeriesItem(stat.productName(), stat.quantity())));
        return series;
    }

    private static void updatePieChartAriaLabel(CustomChart chart,
            String title, DataSeries series) {
        var alt = String.format("%s: %s", title,
                series.getData().stream()
                        .map(d -> String.format("%s %s", d.getName(), d.getY()))
                        .collect(Collectors.joining(", ")));
        chart.setAriaLabel(alt);
    }

    private void updateMonthlyChart(List<MonthlyPurchaseStat> stats) {
        var conf = monthlyChart.getConfiguration();
        var xAxis = new XAxis();
        var months = stats.stream().map(MonthlyPurchaseStat::yearMonth)
                .toArray(String[]::new);
        xAxis.setCategories(months);
        conf.removexAxes();
        conf.addxAxis(xAxis);

        var yAxis = new YAxis();
        yAxis.setTitle(getTranslation(I18n.Storefront.AMOUNT));
        conf.removeyAxes();
        conf.addyAxis(yAxis);

        var series = new DataSeries();
        series.setName(getTranslation(I18n.Storefront.MONTHLY_TOTALS));
        stats.forEach(stat -> series
                .add(new DataSeriesItem(stat.yearMonth(), stat.totalAmount())));
        conf.setSeries(series);

        var alt = String.format("%s: %s",
                getTranslation(I18n.Storefront.MONTHLY_TOTALS),
                stats.stream()
                        .map(s -> String.format("%s %.2f", s.yearMonth(),
                                s.totalAmount()))
                        .collect(Collectors.joining(", ")));
        monthlyChart.setAriaLabel(alt);
    }

    // ---- lifecycle ----------------------------------------------------------

    @Override
    public void enter(ViewChangeEvent event) {
        openingView(event);
        presenter.requestUpdateStats();
    }

    @Override
    public void attach() {
        super.attach();
        ui = getUI();
    }

    @Override
    public void detach() {
        super.detach();
        presenter.cancelUpdateStats();
        ui = null;
    }

    @Override
    public String getTabName() {
        return VIEW_NAME;
    }
}
