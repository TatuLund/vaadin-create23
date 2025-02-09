package org.vaadin.tatu.vaadincreate;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.navigator.View;
import com.vaadin.ui.UI;

@NullMarked
public interface VaadinCreateView extends HasI18N, View {

    /**
     * Sets the title of the current page and announces the opening of a view.
     *
     * @param viewName
     *            the name of the view to be opened
     */
    public default void openingView(String viewName) {
        UI.getCurrent().getPage().setTitle(getTranslation(viewName));
        VaadinCreateUI.get().announce(
                getTranslation(viewName) + " " + getTranslation(I18n.OPENED));
    }
}
