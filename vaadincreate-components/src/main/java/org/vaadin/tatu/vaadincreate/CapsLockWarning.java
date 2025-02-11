package org.vaadin.tatu.vaadincreate;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Safelist;
import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.shared.CapsLockWarningState;

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
        return new CapsLockWarning(field);
    }

    /**
     * Set the message shown, html allowed, will be sanitized.
     *
     * @param message
     *            String value
     */
    public void setMessage(String message) {
        getState().message = sanitize(message);
    }

    @Override
    public CapsLockWarningState getState() {
        return (CapsLockWarningState) super.getState();
    }

    // Sanitize the message as it is set by the user
    private static String sanitize(String unsanitized) {
        var settings = new OutputSettings();
        settings.prettyPrint(false);
        return Jsoup.clean(unsanitized, "", Safelist.relaxed()
                .addAttributes("span", "style").addAttributes("span", "class"),
                settings);
    }
}