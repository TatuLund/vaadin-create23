package org.vaadin.tatu.vaadincreate.admin;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.observability.Telemetry;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

@NullMarked
public interface TabView extends HasI18N {

    /**
     * Returns the name of the tab.
     *
     * @return the name of the tab as a String
     */
    public String getTabName();

    /**
     * Method to be called when the tab is entered.
     */
    public void enter();

    public default void openingView(String viewName) {
        UI.getCurrent().getPage().setTitle(getTranslation(viewName));
        Notification.show(
                String.format("%s %s", getTranslation(viewName),
                        getTranslation(I18n.OPENED)),
                Notification.Type.ASSISTIVE_NOTIFICATION);
        Telemetry.entered(null, (ComponentContainer) this);
    }

}
