package org.vaadin.tatu.vaadincreate.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.AccessTask;
import org.vaadin.tatu.vaadincreate.Html;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.Html.Span;
import org.vaadin.tatu.vaadincreate.auth.CurrentUser;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.backend.data.User;

import com.vaadin.data.Converter;
import com.vaadin.data.HasValue;
import com.vaadin.data.ValueContext;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

@NullMarked
public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

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
        return Html.sanitize(unsanitized);
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
    public static <T, U> U convertToPresentation(@Nullable T value,
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
    public static Span createAvailabilityIcon(Availability availability) {
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
        // Build icon span using Html builder
        return Html.span().cls("v-icon")
                .style("font-family: " + VaadinIcons.CIRCLE.getFontFamily()
                        + ";color:" + color)
                .text(Character.toString(VaadinIcons.CIRCLE.getCodepoint()));
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
    public static void access(@Nullable UI ui, Runnable command) {
        if (ui != null) {
            ui.access(new AccessTask(command));
        } else {
            logger.warn("No UI available for pushing updates.");
        }
    }

    /**
     * Retrieves the current user from the context.
     * 
     * @return the current {@link User} if present
     * @throws IllegalStateException
     *             if no user is present in the context
     */
    public static User getCurrentUserOrThrow() {
        return CurrentUser.get().orElseThrow(
                () -> new IllegalStateException("No user present"));
    }

    /**
     * Start one minute polling for the current UI. This will disable session to
     * expire.
     */
    public static void startPolling() {
        assert UI.getCurrent() != null : "UI should not be null";
        if (UI.getCurrent().getPollInterval() > -1) {
            return;
        }
        UI.getCurrent().setPollInterval(60000);
        logger.debug("Enabled polling");
    }

    /**
     * Stop polling for the current UI. This will enable session to expire.
     */
    public static void stopPolling() {
        assert UI.getCurrent() != null : "UI should not be null";
        if (UI.getCurrent().getPollInterval() > -1) {
            UI.getCurrent().setPollInterval(-1);
            logger.debug("Disabled polling");
        }
    }

    public static <T extends Throwable> boolean throwableHasCause(
            Throwable throwable, Class<T> cause) {
        while (throwable != null) {
            if (throwable.getClass().equals(cause)) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }

    public static Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

}
