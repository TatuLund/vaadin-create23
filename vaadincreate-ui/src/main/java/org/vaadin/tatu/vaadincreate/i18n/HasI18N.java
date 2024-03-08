package org.vaadin.tatu.vaadincreate.i18n;

import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.Cookie;

import org.vaadin.tatu.vaadincreate.util.CookieUtil;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Component;

/**
 * Mixin interface for the views that need translation. Has default
 * implementation of getTranslation method.
 */
public interface HasI18N extends Component {

    /**
     * Get translation with given key and parameter from I18NProvider. Locale is
     * fetched from a "language" Cookie and using system Locale as fallback.
     *
     * @see I18NProvider
     *
     * @param key
     *            Key for the translation
     * @param params
     *            Optional parameters
     * @return String composed with Locale.
     */
    public default String getTranslation(String key, Object... params) {
        return I18NProvider.getInstance().getTranslation(key, fetchLocale(),
                params);
    }

    private Locale fetchLocale() {
        var request = VaadinRequest.getCurrent();
        Locale locale = null;
        // First try to find locale Cookie
        if (request != null) {
            Cookie localeCookie = CookieUtil.getCookieByName("language",
                    request);
            if (localeCookie != null && localeCookie.getValue() != null) {
                Optional<Locale> localeFromCookie = I18NProvider
                        .getInstance().getLocales().stream().filter(loc -> loc
                                .getLanguage().equals(localeCookie.getValue()))
                        .findFirst();
                if (localeFromCookie.isPresent()) {
                    locale = localeFromCookie.get();
                }
            }
        }
        // If no cookie locale, fall back to system default
        return locale == null ? VaadinSession.getCurrent().getLocale() : locale;
    }
}
