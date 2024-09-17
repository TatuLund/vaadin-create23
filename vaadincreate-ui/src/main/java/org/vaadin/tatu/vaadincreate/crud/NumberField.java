package org.vaadin.tatu.vaadincreate.crud;

import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.ErrorLevel;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.TextField;
import org.vaadin.tatu.vaadincreate.AttributeExtension;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.util.Utils;

/**
 * A custom field for handling numeric input with internationalization support.
 * This class extends {@link CustomField} and implements {@link HasI18N}. It
 * uses a {@link TextField} for input and a {@link StockCountConverter} for
 * converting the input value to an integer.
 * 
 * <p>
 * The field is marked as numeric, which affects the virtual keyboard shown on
 * mobile devices.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * NumberField numberField = new NumberField("Enter number");
 * </pre>
 */
@SuppressWarnings({ "serial", "java:S2160" })
public class NumberField extends CustomField<Integer> implements HasI18N {

    private TextField textField = new TextField();
    private StockCountConverter stockCountConverter = new StockCountConverter(
            getTranslation(I18n.Form.CANNOT_CONVERT));
    private Integer intValue;

    /**
     * Creates a new number field with the given caption.
     *
     * @param string
     *            the caption to set
     */
    public NumberField(String string) {
        super();
        setCaption(string);
        setTypeNumber();
        textField.addValueChangeListener(event -> {
            var result = stockCountConverter.convertToModel(
                    textField.getValue(), Utils.createValueContext());
            result.ifError(e -> textField.setComponentError(new UserError(
                    getTranslation(I18n.Form.CANNOT_CONVERT),
                    AbstractErrorMessage.ContentMode.TEXT, ErrorLevel.ERROR)));
            result.ifOk(value -> {
                intValue = value;
                textField.setComponentError(null);
            });
            fireEvent(event);
        });
        textField.setWidth("100%");
    }

    private void setTypeNumber() {
        var stockFieldExtension = new AttributeExtension();
        stockFieldExtension.extend(textField);
        // Mark the stock count field as numeric.
        // This affects the virtual keyboard shown on mobile devices.
        stockFieldExtension.setAttribute("type", "number");
    }

    @Override
    protected TextField initContent() {
        return textField;
    }

    @Override
    protected void doSetValue(Integer value) {
        textField.setValue(stockCountConverter.convertToPresentation(value,
                Utils.createValueContext()));
    }

    @Override
    public Integer getValue() {
        return intValue;
    }
}
