package org.vaadin.tatu.vaadincreate.i18n;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.Cookie;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.vaadin.tatu.vaadincreate.util.CookieUtil;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;

@NullMarked
public interface I18NProvider extends Serializable {

    public static I18NProvider get() {
        return DefaultI18NProvider.getInstance();
    }

    public abstract String getTranslation(String key, Locale locale,
            Object... params);

    public abstract List<Locale> getLocales();

    public static Locale getCurrentSupportedLocale() {
        assert VaadinSession
                .getCurrent() != null : "No VaadinSession available";
        return getSupportedLocale(
                VaadinSession.getCurrent().getLocale().getLanguage())
                        .orElse(DefaultI18NProvider.LOCALE_EN);
    }

    /**
     * Fetches the locale from a cookie named "language" in the current Vaadin
     * request.
     * <p>
     * This method attempts to retrieve the locale information stored in a
     * cookie. If the cookie is found and its value corresponds to a supported
     * locale, that locale is returned. If the cookie is not found or its value
     * does not correspond to a supported locale, null is returned.
     *
     * @return the locale from the "language" cookie if present and supported,
     *         otherwise null
     */
    @Nullable
    public static Locale fetchLocaleFromCookie() {
        var request = VaadinRequest.getCurrent();
        Locale locale = null;
        // First try to find locale Cookie
        if (request != null) {
            Cookie localeCookie = CookieUtil.getCookieByName("language",
                    request);
            if (localeCookie != null && localeCookie.getValue() != null) {
                String lang = localeCookie.getValue();
                Optional<Locale> localeFromCookie = getSupportedLocale(lang);
                if (localeFromCookie.isPresent()) {
                    locale = localeFromCookie.get();
                }
            }
        }
        return locale;
    }

    private static Optional<Locale> getSupportedLocale(String lang) {
        return I18NProvider.get().getLocales().stream()
                .filter(loc -> loc.getLanguage().equals(lang)).findFirst();
    }
}
