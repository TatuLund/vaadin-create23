package org.vaadin.tatu.vaadincreate.crud;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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

/**
 * A custom text field component for filtering book entries. This component
 * extends {@link TextField} and implements {@link HasI18N} and
 * {@link HasAttributes}. It provides a search functionality with a reset button
 * and a keyboard shortcut for quick access.
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * FilterField filterField = new FilterField();
 * </pre>
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class FilterField extends TextField
        implements HasI18N, HasAttributes<FilterField> {

    @Nullable
    private Registration shortcutRegistration;

    /**
     * Constructs a new FilterField with default settings. It sets the ID,
     * style, placeholder, and description for the field, and adds a search
     * icon. A reset button is also included for clearing the filter text.
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
        setAttribute("autocomplete", "off");
        setRole("searchbox");
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
}
