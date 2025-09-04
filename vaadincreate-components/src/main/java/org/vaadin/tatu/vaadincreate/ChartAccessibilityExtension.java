package org.vaadin.tatu.vaadincreate;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.vaadin.annotations.JavaScript;
import com.vaadin.server.AbstractJavaScriptExtension;
import com.vaadin.ui.AbstractComponent;

/**
 * A JavaScript extension for adding accessibility features to Vaadin Chart
 * components. This extension applies WCAG-compliant accessibility patches to
 * individual charts, including keyboard navigation for legend items and context
 * menu buttons.
 */
@NullMarked
@SuppressWarnings("serial")
@JavaScript("chartaccessibility/chart_accessibility_connector.js")
public class ChartAccessibilityExtension extends AbstractJavaScriptExtension {

    protected ChartAccessibilityExtension() {
        // Protected constructor to prevent direct instantiation
    }

    /**
     * Creates and attaches a ChartAccessibilityExtension to the given chart
     * component.
     *
     * @param chart
     *            the chart component to make accessible
     * @param legendsClickable
     *            descriptive text for legend items (e.g., "Click to toggle")
     * @return the extension instance
     */
    public static ChartAccessibilityExtension of(AbstractComponent chart) {
        Objects.requireNonNull(chart, "Chart component cannot be null");
        Objects.requireNonNull(chart.getId(), "Chart ID cannot be null");

        // Check that chart or its superclass is instance of Chart using class
        // name
        if (!isChartComponent(chart)) {
            throw new IllegalArgumentException(
                    "Chart component must be an instance of Chart");
        }

        // Check if extension already exists
        for (var extension : chart.getExtensions()) {
            if (extension instanceof ChartAccessibilityExtension) {
                return (ChartAccessibilityExtension) extension;
            }
        }

        // Create new extension
        var accessibilityExtension = new ChartAccessibilityExtension();
        accessibilityExtension.extend(chart);
        accessibilityExtension.getState().chartId = chart.getId();
        return accessibilityExtension;
    }

    private static boolean isChartComponent(AbstractComponent chart) {
        while (chart != null) {
            if (chart.getClass().getName().contains("Chart")) {
                return true;
            }
            chart = (AbstractComponent) chart.getParent();
        }
        return false;
    }

    @Override
    protected ChartAccessibilityExtensionState getState() {
        return (ChartAccessibilityExtensionState) super.getState();
    }

    @Override
    protected ChartAccessibilityExtensionState getState(boolean markAsDirty) {
        return (ChartAccessibilityExtensionState) super.getState(markAsDirty);
    }

    /**
     * Updates the chart ID.
     *
     * @param chartId
     *            the new chart ID
     */
    public void setChartId(String chartId) {
        Objects.requireNonNull(chartId, "Chart ID cannot be null");
        if (chartId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chart ID cannot be empty");
        }
        getState().chartId = chartId;
    }

    /**
     * Sets the legends clickable text.
     *
     * @param legendsClickable
     *            the new legends clickable text
     */
    public void setLegendsClickable(String legendsClickable) {
        Objects.requireNonNull(legendsClickable,
                "Legends clickable text cannot be null");
        if (legendsClickable.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Legends clickable text cannot be empty");
        }
        getState().legendsClickable = legendsClickable;
    }

    /**
     * Sets the context menu text.
     *
     * @param contextMenu
     */
    public void setContextMenu(String contextMenu) {
        Objects.requireNonNull(contextMenu, "Context menu cannot be null");
        if (contextMenu.trim().isEmpty()) {
            throw new IllegalArgumentException("Context menu cannot be empty");
        }
        getState().contextMenu = contextMenu;
    }

    /**
     * Sets the maximum number of retry attempts.
     *
     * @param maxAttempts
     *            the new maximum number of retry attempts
     */
    public void setMaxAttempts(int maxAttempts) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException(
                    "Max attempts must be a positive integer");
        }
        getState().maxAttempts = maxAttempts;
    }

    /**
     * Sets the retry interval.
     *
     * @param retryInterval
     *            the new retry interval
     */
    public void setRetryInterval(int retryInterval) {
        if (retryInterval <= 0) {
            throw new IllegalArgumentException(
                    "Retry interval must be a positive integer");
        }
        getState().retryInterval = retryInterval;
    }

    /**
     * Gets the current chart ID.
     *
     * @return the chart ID
     */
    @Nullable
    public String getChartId() {
        return getState(false).chartId;
    }

    /**
     * Gets the current legends clickable text.
     *
     * @return the legends clickable text
     */
    public String getLegendsClickable() {
        return getState(false).legendsClickable;
    }

    /**
     * Forces application of accessibility patches, usually called on attach()
     * of the Chart. Also useful when chart content has been dynamically
     * updated.
     */
    public void applyPatches() {
        callFunction("applyPatches");
    }

    /**
     * Sets the menu entries localized texts.
     */
    public void setMenuEntries(List<String> menuEntries) {
        Objects.requireNonNull(menuEntries, "Menu entries cannot be null");
        if (menuEntries.size() != 5) {
            throw new IllegalArgumentException(
                    "Menu entries must contain exactly 5 items");
        }
        getState().menuEntries = menuEntries;
    }

    /**
     * Sets the legend patching behavior. Default true, when false patching
     * legend is not polled.
     *
     * @param patchLegend
     *            boolean
     */
    public void setPatchLegend(boolean patchLegend) {
        getState().patchLegend = patchLegend;
    }

    /**
     * Sets the menu patching behavior. Default true, when false patching menu
     * is not polled.
     *
     * @param patchMenu
     *            boolean
     */
    public void setPatchMenu(boolean patchMenu) {
        getState().patchMenu = patchMenu;
    }
}
