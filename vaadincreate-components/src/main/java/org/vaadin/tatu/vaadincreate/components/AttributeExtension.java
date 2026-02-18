package org.vaadin.tatu.vaadincreate.components;

import java.util.Objects;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
        var state = (AttributeExtensionState) super.getState();
        if (state == null) {
            throw new IllegalStateException("State cannot be null");
        }
        return state;
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
        Objects.requireNonNull(attribute, "Attribute cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        AttributeExtensionState state = getState();
        assert state.attributes != null : "Attributes map cannot be null";
        state.attributes.put(attribute, value);
        assert state.removals != null : "Removals list cannot be null";
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
        assert state.attributes != null : "Attributes map cannot be null";
        state.attributes.remove(attribute);
        assert state.removals != null : "Removals list cannot be null";
        state.removals.add(attribute);
    }

    /**
     * Get the value of the given attribute.
     * <p>
     * Note: This is able to retrieve only the attributes that have been set on
     * the server side. If the attribute was set on the client side using
     * JavaScript, it will not be retrievable through this method.
     *
     * @param attribute
     *            The name of the attribute
     * @return the value of the attribute, or null if not set
     */
    @Nullable
    public String getAttribute(String attribute) {
        AttributeExtensionState state = getState();
        assert state.attributes != null : "Attributes map cannot be null";
        return state.attributes.get(attribute);
    }

    /**
     * Extend the Component with attribute extension.
     *
     * @param target
     *            A Component
     * @return the AttributeExtension instance associated with the target
     *         component, creating it if necessary.
     */
    public static AttributeExtension of(AbstractComponent target) {
        Objects.requireNonNull(target, "Target component cannot be null");
        var optionalAttributeExtension = target.getExtensions().stream()
                .filter(AttributeExtension.class::isInstance)
                .map(AttributeExtension.class::cast).findFirst();
        @Nullable
        AttributeExtension ext = optionalAttributeExtension
                .orElseGet(getExtension(target));
        return ext;
    }

    private static Supplier<@NonNull AttributeExtension> getExtension(
            AbstractComponent target) {
        return () -> {
            AttributeExtension extension = new AttributeExtension();
            extension.extend(target);
            return extension;
        };
    }

    /**
     * Common ARIA attributes.
     * <p>
     * This class provides constants for commonly used ARIA attributes to
     * facilitate their usage in the attribute extension.
     * </p>
     * 
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Attributes">MDN
     *      Web Docs: ARIA Attributes</a>
     */
    public static class AriaAttributes {

        private AriaAttributes() {
            // Private constructor to prevent instantiation
        }

        public static final String ROLE = "role";
        public static final String LABEL = "aria-label";
        public static final String DESCRIBEDBY = "aria-describedby";
        public static final String HIDDEN = "aria-hidden";
        public static final String LABELLEDBY = "aria-labelledby";
        public static final String KEYSHORTCUTS = "aria-keyshortcuts";
        public static final String LIVE = "aria-live";
        public static final String REQUIRED = "aria-required";
        public static final String EXPANDED = "aria-expanded";
        public static final String ATOMIC = "aria-atomic";
    }

    /**
     * Common ARIA roles.
     * <p>
     * This class provides constants for commonly used ARIA roles to facilitate
     * their usage in the attribute extension.
     * </p>
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Roles">MDN
     *      Web Docs: ARIA Roles</a>
     */
    public static class AriaRoles {

        private AriaRoles() {
            // Private constructor to prevent instantiation
        }

        public static final String ALERT = "alert";
        public static final String LINK = "link";
        public static final String FORM = "form";
        public static final String GROUP = "group";
        public static final String DIALOG = "dialog";
        public static final String SEARCHBOX = "searchbox";
        public static final String REGION = "region";
        public static final String FIGURE = "figure";
        public static final String NAVIGATION = "navigation";
        public static final String MAIN = "main";
        public static final String BUTTON = "button"; // Added to support
                                                      // HtmlBuilderTest
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
            Objects.requireNonNull(key, "Attribute key cannot be null");
            Objects.requireNonNull(value, "Attribute value cannot be null");
            getAttributeExtension().setAttribute(key, value);
        }

        /**
         * Sets an attribute with the specified key and value.
         *
         * @param key
         *            the attribute key
         * @param value
         *            the attribute value
         */
        default void setAttribute(String key, boolean value) {
            Objects.requireNonNull(key, "Attribute key cannot be null");
            @Nullable
            String stringValue = String.valueOf(value);
            getAttributeExtension().setAttribute(key,
                    stringValue != null ? stringValue : "");
        }

        /**
         * Sets an attribute with the specified key and value.
         *
         * @param key
         *            the attribute key
         * @param value
         *            the attribute value
         */
        default void setAttribute(String key, int value) {
            Objects.requireNonNull(key, "Attribute key cannot be null");
            @Nullable
            String stringValue = String.valueOf(value);
            getAttributeExtension().setAttribute(key,
                    stringValue != null ? stringValue : "");
        }

        /**
         * Removes an attribute with the specified key.
         *
         * @param key
         *            the attribute key
         */
        default void removeAttribute(String key) {
            Objects.requireNonNull(key, "Attribute key cannot be null");
            getAttributeExtension().removeAttribute(key);
        }

        /**
         * Sets the role attribute for accessibility purposes.
         *
         * @param role
         *            the role value
         */
        default void setRole(String role) {
            Objects.requireNonNull(role, "Role cannot be null");
            setAttribute(AriaAttributes.ROLE, role);
        }

        /**
         * Sets the aria-label attribute for accessibility purposes.
         *
         * @param label
         *            the aria-label value
         */
        default void setAriaLabel(String label) {
            Objects.requireNonNull(label, "ARIA label cannot be null");
            setAttribute(AriaAttributes.LABEL, label);
        }

        /**
         * Gets the value of the specified attribute.
         * <p>
         * This method retrieves the value of an attribute set on the server
         * side.
         *
         * @param key
         *            the attribute key
         * @return the value of the attribute, or null if not set
         */
        @Nullable
        default String getAttribute(String key) {
            Objects.requireNonNull(key, "Attribute key cannot be null");
            return getAttributeExtension().getAttribute(key);
        }
    }
}
