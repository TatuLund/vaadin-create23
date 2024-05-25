package org.vaadin.tatu.vaadincreate.login;

import java.util.Locale;

import org.vaadin.tatu.vaadincreate.i18n.DefaultI18NProvider;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;

import com.vaadin.server.ThemeResource;
import com.vaadin.ui.ComboBox;

@SuppressWarnings("serial")
public class LanguageSelect extends ComboBox<Locale> implements HasI18N {

    private static final String LANGUAGE = "language";

    public LanguageSelect() {
        setCaption(getTranslation(LANGUAGE));
        setWidth(15, Unit.EM);
        setItems(DefaultI18NProvider.getInstance().getLocales());
        setItemCaptionGenerator(item -> getTranslation(item.getLanguage()));
        setItemIconGenerator(locale -> new ThemeResource(
                "flags/" + locale.getLanguage() + ".gif"));
        setEmptySelectionAllowed(false);
        setTextInputAllowed(false);
    }
}
