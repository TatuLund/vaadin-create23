package org.vaadin.tatu.vaadincreate.storefront;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;

import com.vaadin.ui.TextField;

@SuppressWarnings("java:S110")
@NullMarked
class ATextField extends TextField implements HasAttributes<ATextField> {

    /**
     * Predefined autocomplete attribute values for address-related fields.
     */
    public static class Autocomplete {

        private Autocomplete() {
            // Prevent instantiation
        }

        public static final String STREET_ADDRESS = "street-address";
        public static final String POSTAL_CODE = "postal-code";
        public static final String COUNTRY = "country";
        public static final String ADDRESS_LEVEL2 = "address-level2";
    }

    /**
     * Creates a new ATextField with the given caption. This class extends
     * TextField and implements HasAttributes to allow setting custom attributes
     * on the text field component. The caption parameter specifies the text to
     * be displayed as the field's label.
     *
     * @param caption
     *            the text to be displayed as the field's label
     */
    public ATextField(String caption) {
        super(caption);
    }

    /**
     * Sets the autocomplete attribute for this text field to improve user
     * experience by enabling browser autofill features. The value should be a
     * valid autocomplete token as defined in HTML specifications, such as
     * "street-address", "postal-code", "country", etc.
     */
    public void setAutocomplete(String value) {
        setAttribute("autocomplete", value);
    }
}
