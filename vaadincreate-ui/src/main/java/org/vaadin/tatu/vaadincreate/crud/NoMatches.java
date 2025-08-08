package org.vaadin.tatu.vaadincreate.crud;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.ui.Composite;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

@NullMarked
@SuppressWarnings("serial")
public class NoMatches extends Composite
        implements HasI18N, HasAttributes<NoMatches> {

    public NoMatches() {
        var label = new Label(getTranslation(I18n.Books.NO_MATCHES));
        label.addStyleNames(VaadinCreateTheme.BOOKVIEW_NOMATCHES,
                ValoTheme.LABEL_FAILURE);

        // With this attribute screen reader will announce the content of the
        // label when it becomes visible.
        setAttribute(AriaAttributes.LIVE, "assertive");
        setRole(AriaRoles.ALERT);

        setCompositionRoot(label);
    }

}
