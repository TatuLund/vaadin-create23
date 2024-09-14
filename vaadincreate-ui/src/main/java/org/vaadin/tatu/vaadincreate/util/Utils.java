package org.vaadin.tatu.vaadincreate.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Safelist;

import com.vaadin.data.Converter;
import com.vaadin.data.HasValue;
import com.vaadin.data.ValueContext;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

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
}
