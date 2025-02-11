package org.vaadin.tatu.vaadincreate.crud;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.AttributeExtension;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.ui.Composite;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

@NullMarked
@SuppressWarnings("serial")
public class NoMatches extends Composite implements HasI18N {

    public NoMatches() {
        var label = new Label(getTranslation(I18n.Books.NO_MATCHES));
        label.addStyleNames(VaadinCreateTheme.BOOKVIEW_NOMATCHES,
                ValoTheme.LABEL_FAILURE);

        var attributes = AttributeExtension.of(label);
        // With this attribute screen reader will announce the content of the
        // label when it becomes visible.
        attributes.setAttribute("aria-live", "assertive");
        attributes.setAttribute("role", "alert");

        setCompositionRoot(label);
    }

}
