package org.vaadin.tatu.vaadincreate;

import com.vaadin.annotations.JavaScript;
import com.vaadin.server.AbstractJavaScriptExtension;
import com.vaadin.ui.TextField;

/**
 * A JavaScript extension for adding arbitrary HTML attributes for components.
 */
@SuppressWarnings("serial")
@JavaScript("attributeextension/attribute_extension_connector.js")
public class AttributeExtension extends AbstractJavaScriptExtension {

    /**
     * Extend the TextField with attribute extension.
     *
     * @param target
     *            A TextField
     */
    public void extend(TextField target) {
        super.extend(target);
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
    }

    /**
     * Remove the given attribute.
     *
     * @param attribute
     *            The name of the attribute
     */
    public void removeAttribute(String attribute) {
        getState().attributes.remove(attribute);
    }
}
