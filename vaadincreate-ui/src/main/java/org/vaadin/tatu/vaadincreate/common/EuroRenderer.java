package org.vaadin.tatu.vaadincreate.common;

import java.text.NumberFormat;
import java.util.Locale;

import org.jspecify.annotations.NullMarked;

import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.NumberRenderer;

/**
 * Number renderer for euro amounts using locale-specific thousand and decimal
 * separators. Values are always shown with two fraction digits and suffixed
 * with the euro sign.
 */
@NullMarked
@SuppressWarnings("serial")
public class EuroRenderer extends NumberRenderer {

    /**
     * Creates a EuroRenderer using the current UI locale when available,
     * otherwise the default JVM locale.
     */
    public EuroRenderer() {
        super(createEuroFormat(currentLocale()));
    }

    /**
     * Creates a EuroRenderer for the given locale.
     *
     * @param locale
     *            the locale to use for number formatting
     */
    public EuroRenderer(Locale locale) {
        super(createEuroFormat(locale));
    }

    /**
     * Creates a NumberFormat for euro values using the given locale. The format
     * uses locale-specific grouping and decimal separators, fixes the number of
     * fraction digits to two and appends the euro sign.
     */
    public static NumberFormat createEuroFormat(Locale locale) {
        return EuroConverter.createEuroFormat(locale);
    }

    private static Locale currentLocale() {
        UI ui = UI.getCurrent();
        return ui != null ? ui.getLocale() : Locale.getDefault();
    }
}
