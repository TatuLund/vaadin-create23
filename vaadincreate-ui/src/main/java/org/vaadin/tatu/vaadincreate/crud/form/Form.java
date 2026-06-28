package org.vaadin.tatu.vaadincreate.crud.form;

import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;

import com.vaadin.ui.VerticalLayout;

/**
 * A form component that extends VerticalLayout and sets ARIA attributes to
 * enhance accessibility. The form is given a tabindex, an aria-label for screen
 * readers, a role of "form", and aria-keyshortcuts for keyboard navigation.
 */
@SuppressWarnings("java:S110")
public class Form extends VerticalLayout implements HasAttributes<Form> {

    /**
     * Constructs a new Form instance and sets ARIA attributes to enhance
     * accessibility.
     */
    public Form() {
        super();
        // Set ARIA attributes for the form to make it accessible
        setAttribute("tabindex", 0);
        setRole(AriaRoles.FORM);
        setAttribute(AriaAttributes.KEYSHORTCUTS, "Escape PageDown PageUp");
    }
}
