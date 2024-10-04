package org.vaadin.tatu.vaadincreate.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;

import com.vaadin.data.Converter;
import com.vaadin.data.HasValue;
import com.vaadin.data.ValueContext;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.UIDetachedException;

public class Utils {

    private Utils() {
        // private constructor to hide the implicit public one
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
        return Jsoup.clean(unsanitized, "", Safelist.relaxed()
                .addAttributes("span", "style").addAttributes("span", "class"),
                settings);
    }

    /**
     * Fixes the session fixation vulnerability by changing the session ID. This
     * method disables push mode, changes the session ID, and enables push mode
     * again.
     */
    public static void sessionFixation() {
        UI.getCurrent().getPushConfiguration().setPushMode(PushMode.DISABLED);
        VaadinServletRequest request = (VaadinServletRequest) VaadinRequest
                .getCurrent();
        request.getHttpServletRequest().changeSessionId();
        UI.getCurrent().getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
    }

    /**
     * Formats the given LocalDateTime object into a string representation using
     * the specified locale.
     *
     * @param dateTime
     *            the LocalDateTime object to be formatted
     * @param locale
     *            the locale to be used for formatting
     * @return the formatted string representation of the LocalDateTime object
     */
    public static String formatDate(LocalDateTime dateTime, Locale locale) {
        var formatter = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale);
        return dateTime.format(formatter);
    }

    /**
     * Helper method to create a ValueContext for a Converter
     *
     * @return ValueContext
     */
    public static ValueContext createValueContext() {
        var field = new TextField();
        return new ValueContext(field, field);
    }

    /**
     * Converts a given value to its presentation form using the specified
     * converter.
     *
     * @param <T>
     *            the type of the value to be converted
     * @param <U>
     *            the type of the presentation value
     * @param value
     *            the value to be converted
     * @param converter
     *            the converter to use for the conversion
     * @return the converted presentation value
     */
    public static <T, U> U convertToPresentation(T value,
            Converter<U, T> converter) {
        return converter.convertToPresentation(value, createValueContext());
    }

    /**
     * Sets the value of the given field if the new value is different from the
     * current value.
     *
     * @param <T>
     *            the type of the value
     * @param field
     *            the field whose value is to be set
     * @param newValue
     *            the new value to set if it is different from the current value
     */
    public static <T> void setValueIfDifferent(HasValue<T> field, T newValue) {
        if (!field.getValue().equals(newValue)) {
            field.setValue(newValue);
        }
    }

    /**
     * Creates an icon for the given availability status.
     *
     * @param availability
     *            the availability status for which to create an icon
     * @return the HTML representation of the icon
     */
    public static String createAvailabilityIcon(Availability availability) {
        var color = "";
        switch (availability) {
        case AVAILABLE:
            color = VaadinCreateTheme.COLOR_AVAILABLE;
            break;
        case COMING:
            color = VaadinCreateTheme.COLOR_COMING;
            break;
        case DISCONTINUED:
            color = VaadinCreateTheme.COLOR_DISCONTINUED;
            break;
        default:
            break;
        }

        return String.format(
                "<span class='v-icon' style='font-family: %s;color:%s'>&#x%s;</span>",
                VaadinIcons.CIRCLE.getFontFamily(), color,
                Integer.toHexString(VaadinIcons.CIRCLE.getCodepoint()));
    }

    /**
     * Executes a given command within the context of the specified UI. If the
     * UI is not available, logs a warning message. If the UI is detached (e.g.,
     * the browser window is closed), logs a warning message.
     *
     * @param ui
     *            the UI instance within which the command should be executed
     * @param command
     *            the command to be executed
     */
    public static void access(UI ui, Runnable command) {
        if (ui != null) {
            try {
                ui.access(command);
            } catch (UIDetachedException e) {
                logger.warn("Browser window was closed while pushing updates.");
            }
        } else {
            logger.warn("No UI available for pushing updates.");
        }
    }

    private static Logger logger = LoggerFactory.getLogger(Utils.class);

}
