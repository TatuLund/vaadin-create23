package org.vaadin.tatu.vaadincreate.common;

import java.util.Arrays;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.components.ChartAccessibilityExtension;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.addon.charts.Chart;
import com.vaadin.addon.charts.model.Buttons;
import com.vaadin.addon.charts.model.ChartType;
import com.vaadin.addon.charts.model.Exporting;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Component.Focusable;

/**
 * CustomChart is an extension of the Chart class that allows for setting custom
 * attributes.
 * <p>
 * It uses the AttributeExtension to manage these attributes. Furthermore, it
 * implements {@link Focusable} to allow focus management and {@link HasI18N}
 * for internationalization support. Also, it integrates
 * {@link ChartAccessibilityExtension} to enhance accessibility features for the
 * chart.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class CustomChart extends Chart
        implements HasAttributes<CustomChart>, Focusable, HasI18N {

    private static final String CHARTS_EXPORT_SERVICE_URL = "http://charts:export@127.0.0.1:8083/";

    @Nullable
    private ChartAccessibilityExtension a11y;
    private int tabIndex = -1;

    public CustomChart(ChartType type) {
        super(type);
        a11y = ChartAccessibilityExtension.of(this);
        a11y.setLegendsClickable(getTranslation(I18n.Stats.LEGEND_CLICKABLE));
        a11y.setContextMenu(getTranslation(I18n.Stats.CONTEXT_MENU));
        a11y.setMenuEntries(
                Arrays.asList(
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
                    var chart = document.getElementById('%s');
                    if (chart) { chart.focus(); }
                }, 100);
                """.formatted(getId()));
    }

    /**
     * Enables exporting functionality for the chart, allowing users to export
     * the chart in various formats. This method configures the exporting
     * options, including setting a custom file name for the exported chart and
     * specifying the URL of the export service. It also adds custom buttons to
     * the exporting menu.
     */
    public void enableExporting() {
        Objects.requireNonNull(getConfiguration(),
                "Chart configuration must not be null to enable exporting");
        // Create the export configuration
        var exporting = new Exporting(true);
        // Customize the file name of the download file
        exporting.setFilename("chart");
        // Use the exporting configuration in the chart, note in the
        // real production environment the URL should point to a
        // actual address where the application is hosted instead
        // of localhost.
        exporting.setUrl(CHARTS_EXPORT_SERVICE_URL);
        exporting.setButtons(new Buttons());
        getConfiguration().setExporting(exporting);
    }
}
