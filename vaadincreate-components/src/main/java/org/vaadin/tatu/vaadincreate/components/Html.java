package org.vaadin.tatu.vaadincreate.components;

import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Safelist;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;

/**
 * Minimal HTML builder for server-side composition of small, trusted fragments.
 * Escapes text &amp; attribute values, allows raw HTML insertion for trusted
 * icon snippets.
 */
@NullMarked
public abstract class Html<T extends Html<T>> {
    protected final String name;
    protected final Map<String, String> attrs = new LinkedHashMap<>();
    protected final List<Object> children = new ArrayList<>();
    protected @Nullable String text;

    protected Html(String name) {
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }

    /**
     * Set tabindex attribute
     * 
     * @param index
     *            the tabindex value
     * @return this
     */
    public T tabindex(int index) {
        return attr("tabindex", Integer.toString(index));
    }

    /**
     * Set an attribute key/value pair
     * 
     * @param key
     *            the attribute name
     * @param value
     *            the attribute value
     * @return this
     */
    public T attr(String key, String value) {
        // Value assumed non-null by callers; empty ignored for cleanliness
        if (!value.isEmpty()) {
            attrs.put(key, value);
        }
        return self();
    }

    /**
     * Set the role attribute
     *
     * @param role
     *            the role value, use AriaRoles constants
     * @return this
     */
    public T role(String role) {
        return attr(AriaAttributes.ROLE, role);
    }

    /**
     * Set the CSS class attribute
     *
     * @param classes
     *            the CSS classes to set
     * @return this
     */
    public T cls(String... classes) {
        // Assume caller passes non-null; ignore empty array
        if (classes.length > 0) {
            attr("class", String.join(" ", classes));
        }
        return self();
    }

    /**
     * Set the style attribute
     * 
     * @param style
     *            the style value
     * @return this
     */
    public T style(String style) {
        return attr("style", style);
    }

    /**
     * Set the id attribute
     * 
     * @param id
     *            the id value
     * @return this
     */
    public T id(String id) {
        return attr("id", id);
    }

    /**
     * Set text content (escaped)
     * 
     * @param txt
     *            the text content
     * @return this
     */
    public T text(String txt) {
        this.text = txt;
        return self();
    }

    /**
     * Add a child tag
     * 
     * @param child
     *            the child tag to add
     * @return this
     */
    public T add(Html<?> child) {
        children.add(child);
        return self();
    }

    /**
     * Add raw trusted HTML (NOT escaped). Use sparingly for icon glyphs.
     * 
     * @param trustedHtml
     *            the raw HTML content
     * @return this
     */
    public T raw(String trustedHtml) {
        // Assume non-null trusted fragment; skip if empty
        if (!trustedHtml.isEmpty()) {
            children.add(new Raw(trustedHtml));
        }
        return self();
    }

    /**
     * Build HTML string and sanitize it with Jsoup.
     *
     * @return the built HTML string
     */
    public String build() {
        var sb = new StringBuilder();
        sb.append('<').append(name);
        attrs.forEach((k, v) -> sb.append(' ').append(k).append('=').append('"')
                .append(escape(v)).append('"'));
        // Only void tags (currently just <br>) self-close. Others must have
        // explicit closing tag
        boolean isVoid = this instanceof Br;
        if (isVoid) {
            sb.append('/').append('>');
            return sb.toString();
        }
        sb.append('>');
        if (text != null) {
            sb.append(escape(text));
        }
        for (var child : children) {
            if (child instanceof Raw) {
                sb.append(((Raw) child).html);
            } else if (child instanceof Html) {
                sb.append(((Html<?>) child).build());
            }
        }
        sb.append("</").append(name).append('>');
        return sanitize(sb.toString());
    }

    /**
     * Sanitizes the given string by removing any potentially unsafe HTML tags
     * and attributes.
     *
     * @param unsanitized
     *            the string to be sanitized
     * @return the sanitized string
     */
    public static String sanitize(String unsanitized) {
        var settings = new OutputSettings();
        settings.prettyPrint(false);
         // @formatter:off
       var safelist = Safelist.relaxed().addAttributes(":all",
                "id",
                "class",
                "style",
                "tabindex");
        // @formatter:on
        // Add all constants in AriaAttributes to safelist
        for (var field : AriaAttributes.class.getDeclaredFields()) {
            try {
                var attrName = (String) field.get(null);
                safelist.addAttributes(":all", attrName);
            } catch (IllegalAccessException e) {
                // Ignore
            }
        }
        return Jsoup.clean(unsanitized, "", safelist, settings);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static final class Raw {
        final String html;

        Raw(String h) {
            this.html = h;
        }
    }

    // Factories
    public static Span span() {
        return new Span();
    }

    public static Div div() {
        return new Div();
    }

    public static H1 h1() {
        return new H1();
    }

    public static B b() {
        return new B();
    }

    public static Br br() {
        return new Br();
    }

    // Concrete tags
    public static final class Span extends Html<Span> {
        private Span() {
            super("span");
        }
    }

    public static final class Div extends Html<Div> {
        private Div() {
            super("div");
        }
    }

    public static final class B extends Html<B> {
        private B() {
            super("b");
        }
    }

    public static final class H1 extends Html<H1> {
        private H1() {
            super("h1");
        }
    }

    public static final class Br extends Html<Br> {
        private Br() {
            super("br");
        }
    }
}
