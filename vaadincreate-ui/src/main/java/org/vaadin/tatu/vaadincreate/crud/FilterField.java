package org.vaadin.tatu.vaadincreate.crud;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.AttributeExtension;
import org.vaadin.tatu.vaadincreate.ResetButtonForTextField;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutAction.ModifierKey;
import com.vaadin.event.FocusShortcut;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.Registration;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class FilterField extends TextField implements HasI18N, HasAttributes {

    @Nullable
    private Registration shortcutRegistration;
    private AttributeExtension attributes;

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
        attributes = AttributeExtension.of(this);
        setAttribute("autocomplete", "off");
        setAttribute("role", "searchbox");
        setShortcut();
    }

    @SuppressWarnings("java:S3878")
    private void setShortcut() {
        shortcutRegistration = addShortcutListener(new FocusShortcut(this,
                KeyCode.F, new int[] { ModifierKey.CTRL }));
    }

    @Override
    public void detach() {
        super.detach();
        shortcutRegistration.remove();
    }

    @Override
    public AttributeExtension getAttributeExtension() {
        return attributes;
    }
}
