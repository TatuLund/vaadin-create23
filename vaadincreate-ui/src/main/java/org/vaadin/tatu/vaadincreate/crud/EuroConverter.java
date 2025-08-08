package org.vaadin.tatu.vaadincreate.crud;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.jspecify.annotations.NullMarked;

import com.vaadin.data.Result;
import com.vaadin.data.ValueContext;
import com.vaadin.data.converter.StringToBigDecimalConverter;

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
    public Result<BigDecimal> convertToModel(String value,
            ValueContext context) {
        value = value.replaceAll("[€\\s]", "").trim();
        if ("".equals(value)) {
            value = "0";
        }
        return super.convertToModel(value, context);
    }

    @Override
    protected NumberFormat getFormat(Locale locale) {
        // Always display currency with two decimals
        var format = super.getFormat(locale);
        if (format instanceof DecimalFormat decimalFormat) {
            decimalFormat.setMaximumFractionDigits(2);
            decimalFormat.setMinimumFractionDigits(2);
            decimalFormat.setRoundingMode(java.math.RoundingMode.HALF_UP);
        }
        return format;
    }

    @Override
    public String convertToPresentation(BigDecimal value,
            ValueContext context) {
        return String.format("%s €",
                super.convertToPresentation(value, context));
    }
}
