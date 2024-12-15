package org.vaadin.tatu.vaadincreate.crud;

import org.vaadin.tatu.vaadincreate.AttributeExtension;
import org.vaadin.tatu.vaadincreate.ResetButtonForTextField;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class FilterField extends TextField implements HasI18N {

    /**
     * A custom text field component for filtering book entries.
     */
    public FilterField() {
        setId("filter-field");
        setStyleName(VaadinCreateTheme.BOOKVIEW_FILTER);
        addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        setPlaceholder(getTranslation(I18n.Books.FILTER));
        setDescription(getTranslation(I18n.Books.FILTER));
        setIcon(VaadinIcons.SEARCH);
        var resetButton = ResetButtonForTextField.of(this);
        resetButton.setButtonLabel(getTranslation(I18n.Books.CLEAR_TEXT));
        var attributes = AttributeExtension.of(this);
        attributes.setAttribute("autocomplete", "off");
        attributes.setAttribute("role", "searchbox");
    }
}
