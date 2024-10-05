package org.vaadin.tatu.vaadincreate.crud.form;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import com.vaadin.data.Result;
import com.vaadin.data.ValueContext;
import com.vaadin.data.converter.StringToIntegerConverter;

/**
 * A converter that extends {@link StringToIntegerConverter} to handle stock
 * count conversions. This converter ensures that the number format does not use
 * a thousands separator and always parses the input as an integer.
 */
@SuppressWarnings("serial")
public class StockCountConverter extends StringToIntegerConverter {

    public StockCountConverter(String message) {
        super(message);
    }

    @Override
    protected NumberFormat getFormat(Locale locale) {
        // do not use a thousands separator, as HTML5 input type
        // number expects a fixed wire/DOM number format regardless
        // of how the browser presents it to the user (which could
        // depend on the browser locale)
        var format = new DecimalFormat();
        format.setMaximumFractionDigits(0);
        format.setDecimalSeparatorAlwaysShown(false);
        format.setParseIntegerOnly(true);
        format.setGroupingUsed(false);
        return format;
    }

    @Override
    public Result<Integer> convertToModel(String value, ValueContext context) {
        Result<Integer> result = super.convertToModel(value, context);
        return result.map(stock -> stock == null ? 0 : stock);
    }

}
