package org.vaadin.tatu.vaadincreate.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.jspecify.annotations.NullMarked;

import com.vaadin.data.ValueContext;
import com.vaadin.data.converter.StringToBigDecimalConverter;
import com.vaadin.ui.UI;

/**
 * A converter that adds/removes the euro sign and formats currencies with two
 * decimal places.
 */
@NullMarked
@SuppressWarnings("serial")
public class EuroConverter extends StringToBigDecimalConverter {

    /**
     * Constructs a EuroConverter.
     *
     * @param message
     *            the error message to display when conversion fails
     */
    public EuroConverter(String message) {
        super(message);
    }

    @Override
    protected NumberFormat getFormat(Locale locale) {
        // Always display currency with two decimals
        var format = super.getFormat(locale);
        if (format instanceof DecimalFormat decimalFormat) {
            decimalFormat.setMaximumFractionDigits(2);
            decimalFormat.setMinimumFractionDigits(2);
            decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
            decimalFormat.setPositiveSuffix(" €");
            decimalFormat.setNegativeSuffix(" €");
        }
        return format;
    }

    @Override
    public String convertToPresentation(BigDecimal value,
            ValueContext context) {
        return String.format("%s", super.convertToPresentation(value, context));
    }

    public static NumberFormat createEuroFormat(Locale locale) {
        var converter = new EuroConverter("");
        return converter.getFormat(locale);
    }

    public static NumberFormat createEuroFormat() {
        return createEuroFormat(currentLocale());
    }

    private static Locale currentLocale() {
        UI ui = UI.getCurrent();
        return ui != null ? ui.getLocale() : Locale.getDefault();
    }
}
