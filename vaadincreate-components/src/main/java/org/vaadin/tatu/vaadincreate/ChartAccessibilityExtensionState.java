package org.vaadin.tatu.vaadincreate;

import java.util.List;

import com.vaadin.shared.JavaScriptExtensionState;

/**
 * Shared state for ChartAccessibilityExtension. Contains the chart ID and
 * legend accessibility configuration.
 */
@SuppressWarnings({ "serial", "java:S1104", "java:S1319" })
public class ChartAccessibilityExtensionState extends JavaScriptExtensionState {

    @SuppressWarnings("java:S1948")
    public List<String> menuEntries = List.of("Print chart",
            "Download as PNG image", "Download as JPEG image",
            "Download as PDF document", "Download as SVG image");

    public String legendsClickable = "Toggle series visibility";

    public String contextMenu = "Chart context menu";

    public int maxAttempts = 20;

    /**
     * Interval between retry attempts in milliseconds.
     */
    public int retryInterval = 100;
}
