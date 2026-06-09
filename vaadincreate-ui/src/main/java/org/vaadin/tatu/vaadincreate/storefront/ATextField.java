package org.vaadin.tatu.vaadincreate.storefront;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;

import com.vaadin.ui.TextField;

@SuppressWarnings("java:S110")
@NullMarked
class ATextField extends TextField implements HasAttributes<ATextField> {

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
