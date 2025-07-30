package org.vaadin.tatu.vaadincreate.crud.form;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.AttributeExtension;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.AttributeExtension.HasAttributes;

import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.JavaScript;

/**
 * Side panel layout with CSS animation sliding in / out.
 */
@NullMarked
@SuppressWarnings({ "serial", "java:S2160" })
public class SidePanel extends Composite implements HasAttributes {
    private static final String ARIA_HIDDEN = "aria-hidden";
    private CssLayout layout = new CssLayout();
    private AttributeExtension attributes;

    /**
     * Constructs a new SidePanel instance.
     *
     * This constructor initializes the layout with a specific ID and applies
     * multiple style names from the VaadinCreateTheme. It then sets the
     * composition root of the component to the initialized layout.
     */
    public SidePanel() {
        layout.setId("book-form");
        layout.addStyleNames(VaadinCreateTheme.BOOKFORM,
                VaadinCreateTheme.BOOKFORM_WRAPPER);
        attributes = AttributeExtension.of(layout);
        setAttribute("role", "dialog");
        setAttribute("aria-label", "side panel");
        setAttribute(ARIA_HIDDEN, "true");

        setCompositionRoot(layout);
    }

    /**
     * Set side panel content.
     *
     * @param content
     *            Component
     */
    public void setContent(Component content) {
        layout.removeAllComponents();
        layout.addComponent(content);
    }

    /**
     * Sets the ARIA label attribute of the component.
     *
     * <p>
     * This method allows you to specify a descriptive label for the component,
     * enhancing accessibility by providing assistive technologies with a text
     * description of the element.
     *
     * @param label
     *            the ARIA label to be set on the component.
     */
    public void setAriaLabel(String label) {
        setAttribute("aria-label", label);
    }

    /**
     * Show the side panel with the animation. True slides in, false slides out.
     *
     * @param visible
     *            boolean value.
     */
    public void show(boolean visible) {
        // This process is tricky. The element needs to be in DOM and not
        // having 'display: none' in order to CSS animations to work. We will
        // set display none after a delay so that pressing 'tab' key will not
        // reveal the form while set not visible.
        if (visible) {
            JavaScript.eval(
                    "document.getElementById('book-form').style.display='block';");
            if (getUI() != null) {
                getUI().runAfterRoundTrip(() -> {
                    layout.addStyleName(
                            VaadinCreateTheme.BOOKFORM_WRAPPER_VISIBLE);
                    setAttribute(ARIA_HIDDEN, "true");
                });
            }
        } else {
            layout.removeStyleName(VaadinCreateTheme.BOOKFORM_WRAPPER_VISIBLE);
            setAttribute(ARIA_HIDDEN, "false");
            if (isAttached()) {
                getUI().runAfterRoundTrip(() -> JavaScript.eval(
                        "setTimeout(() => document.getElementById('book-form').style.display='none', 200);"));
            }
        }
    }

    @Override
    public AttributeExtension getAttributeExtension() {
        return attributes;
    }
}
