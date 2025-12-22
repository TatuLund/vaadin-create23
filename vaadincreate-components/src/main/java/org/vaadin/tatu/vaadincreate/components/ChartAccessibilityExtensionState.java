package org.vaadin.tatu.vaadincreate.components;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.vaadin.shared.JavaScriptExtensionState;

/**
 * Shared state for ChartAccessibilityExtension. Contains the chart ID and
 * legend accessibility configuration.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S1104", "java:S1319" })
public class ChartAccessibilityExtensionState extends JavaScriptExtensionState {

    @SuppressWarnings("java:S1948")
    @Nullable
    public List<String> menuEntries = List.of("Print chart",
            "Download as PNG image", "Download as JPEG image",
            "Download as PDF document", "Download as SVG image");

    @Nullable
    public String legendsClickable = "Toggle series visibility";

    @Nullable
    public String contextMenu = "Chart context menu";

    public int maxAttempts = 20;

    /**
     * Interval between retry attempts in milliseconds.
     */
    public int retryInterval = 100;
}
