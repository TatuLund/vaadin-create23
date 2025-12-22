package org.vaadin.tatu.vaadincreate.components;

import java.util.Objects;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Safelist;
import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.components.shared.CapsLockWarningState;

import com.vaadin.server.AbstractExtension;
import com.vaadin.ui.PasswordField;

@NullMarked
@SuppressWarnings("serial")
public class CapsLockWarning extends AbstractExtension {

    protected CapsLockWarning(PasswordField field) {
        // Non-public constructor to discourage direct instantiation
        extend(field);
    }

    /**
     * Add CapsLock warning observer to PasswordField
     * 
     * @param field
     *            a PasswordField
     * @return CapsLockWarning extension
     */
    public static CapsLockWarning warnFor(PasswordField field) {
        Objects.requireNonNull(field, "Field cannot be null");
        return new CapsLockWarning(field);
    }

    /**
     * Set the message shown, html allowed, will be sanitized.
     *
     * @param message
     *            String value
     */
    public void setMessage(String message) {
        Objects.requireNonNull(message, "Message cannot be null");
        getState().message = sanitize(message);
    }

    @Override
    public CapsLockWarningState getState() {
        var state = (CapsLockWarningState) super.getState();
        if (state == null) {
            throw new IllegalStateException("State cannot be null");
        }
        return state;
    }

    // Sanitize the message as it is set by the user
    private static String sanitize(String unsanitized) {
        var settings = new OutputSettings();
        settings.prettyPrint(false);
        var cleaned = Jsoup.clean(unsanitized, "", Safelist.relaxed()
                .addAttributes("span", "style").addAttributes("span", "class"),
                settings);
        if (cleaned == null) {
            cleaned = "";
        }
        return cleaned;
    }
}