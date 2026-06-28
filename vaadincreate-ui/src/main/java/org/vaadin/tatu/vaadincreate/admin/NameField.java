package org.vaadin.tatu.vaadincreate.admin;

import org.vaadin.tatu.vaadincreate.backend.data.Category;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.HasAttributes;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.TextField;

@SuppressWarnings("java:S110")
class NameField extends TextField implements HasI18N, HasAttributes<NameField> {

    public NameField(Category category) {
        super();
        setAttribute("autocomplete", "off");
        setAriaLabel(getTranslation(I18n.Category.CATEGORY_NAME));
        removeAttribute("aria-labelledby");
        setId(String.format("name-%s", category.getId()));
        setValueChangeMode(ValueChangeMode.LAZY);
        setValueChangeTimeout(2000);
        setWidthFull();
        setPlaceholder(getTranslation(I18n.Category.INSTRUCTION));
    }
}
