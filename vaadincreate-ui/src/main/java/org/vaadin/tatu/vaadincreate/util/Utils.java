package org.vaadin.tatu.vaadincreate.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Safelist;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.shared.communication.PushMode;
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
}
