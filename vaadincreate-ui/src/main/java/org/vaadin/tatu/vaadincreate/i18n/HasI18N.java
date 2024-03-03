package org.vaadin.tatu.vaadincreate.i18n;

import com.vaadin.server.VaadinService;

public interface HasI18N {

    public default String getTranslation(String key, Object... params) {
        var browserLocale = VaadinService.getCurrentRequest().getLocale();
        ;
        return I18NProvider.getInstance().getTranslation(key, browserLocale,
                params);
    }
}
