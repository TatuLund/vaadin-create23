package org.vaadin.tatu.vaadincreate.common;

import java.util.Arrays;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.components.ChartAccessibilityExtension;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.addon.charts.Chart;
import com.vaadin.addon.charts.model.ChartType;
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
}
