package org.vaadin.tatu.vaadincreate.i18n;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.Cookie;

import org.vaadin.tatu.vaadincreate.util.CookieUtil;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;

public interface I18NProvider extends Serializable {

    public static I18NProvider get() {
        return DefaultI18NProvider.getInstance();
    }

    public abstract String getTranslation(String key, Locale locale,
            Object... params);

    public abstract List<Locale> getLocales();

    public static Locale getCurrentSupportedLocale() {
        return getSupportedLocale(
                VaadinSession.getCurrent().getLocale().getLanguage())
                        .orElse(DefaultI18NProvider.LOCALE_EN);
    }

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
