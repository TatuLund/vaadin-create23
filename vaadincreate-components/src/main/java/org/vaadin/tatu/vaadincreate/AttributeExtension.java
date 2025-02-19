package org.vaadin.tatu.vaadincreate;

import org.jspecify.annotations.NullMarked;

import com.vaadin.annotations.JavaScript;
import com.vaadin.server.AbstractJavaScriptExtension;
import com.vaadin.ui.AbstractComponent;

/**
 * A JavaScript extension for adding arbitrary HTML attributes for component's
 * top level element.
 */
@NullMarked
@SuppressWarnings("serial")
@JavaScript("attributeextension/attribute_extension_connector.min.js")
public class AttributeExtension extends AbstractJavaScriptExtension {

    protected AttributeExtension() {
        // Non-public constructor to discourage direct instantiation
    }

    @Override
    protected AttributeExtensionState getState() {
        return (AttributeExtensionState) super.getState();
    }

    /**
     * Set the attribute to TextField's internal input element.
     *
     * @param attribute
     *            The name of the attribute
     * @param value
     *            The value for the attribute
     */
    public void setAttribute(String attribute, String value) {
        getState().attributes.put(attribute, value);
        getState().removals.remove(attribute);
    }

    /**
     * Remove the given attribute.
     *
     * @param attribute
     *            The name of the attribute
     */
    public void removeAttribute(String attribute) {
        getState().attributes.remove(attribute);
        getState().removals.add(attribute);
    }

    /**
     * Extend the Component with attribute extension.
     *
     * @param target
     *            A Component
     */
    public static AttributeExtension of(AbstractComponent target) {
        var extension = new AttributeExtension();
        extension.extend(target);
        return extension;
    }
}
