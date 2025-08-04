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
        // Constructor is protected to discourage direct instantiation; use the
        // static factory method 'of' instead.
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
        AttributeExtensionState state = getState();
        state.attributes.put(attribute, value);
        state.removals.remove(attribute);
    }

    /**
     * Remove the given attribute.
     *
     * @param attribute
     *            The name of the attribute
     */
    public void removeAttribute(String attribute) {
        AttributeExtensionState state = getState();
        state.attributes.remove(attribute);
        state.removals.add(attribute);
    }

    /**
     * Extend the Component with attribute extension.
     *
     * @param target
     *            A Component
     */
    public static AttributeExtension of(AbstractComponent target) {
        var optionalAttributeExtension = target.getExtensions().stream()
                .filter(ext -> ext instanceof AttributeExtension).findFirst();
        if (optionalAttributeExtension.isPresent()) {
            return (AttributeExtension) optionalAttributeExtension.get();
        }
        var extension = new AttributeExtension();
        extension.extend(target);
        return extension;
    }

    /**
     * Interface to be implemented by components that support attributes.
     * Provides methods to set and remove attributes.
     * <p>
     * This interface is generic and enforces that only subclasses of
     * AbstractComponent can implement it, ensuring type safety for attribute
     * extension operations.
     *
     * @param <T>
     *            the type of the component, must extend AbstractComponent
     */
    public interface HasAttributes<T extends AbstractComponent> {

        /**
         * Gets the AttributeExtension associated with this component.
         *
         * @return the AttributeExtension instance
         */
        @SuppressWarnings("unchecked")
        private AttributeExtension getAttributeExtension() {
            return AttributeExtension.of((T) this);
        }

        /**
         * Sets an attribute with the specified key and value.
         *
         * @param key
         *            the attribute key
         * @param value
         *            the attribute value
         */
        default void setAttribute(String key, String value) {
            getAttributeExtension().setAttribute(key, value);
        }

        /**
         * Removes an attribute with the specified key.
         *
         * @param key
         *            the attribute key
         */
        default void removeAttribute(String key) {
            getAttributeExtension().removeAttribute(key);
        }
    }
}
