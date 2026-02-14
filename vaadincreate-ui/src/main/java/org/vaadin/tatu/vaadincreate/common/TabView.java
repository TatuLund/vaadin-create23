package org.vaadin.tatu.vaadincreate.common;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;
import org.vaadin.tatu.vaadincreate.observability.Telemetry;

import com.vaadin.event.ConnectorEventListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.util.ReflectTools;

/**
 * Interface for admin tab views.
 */
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
    public void enter(ViewChangeEvent event);

    /**
     * Default implementation for opening view event.
     *
     * @param event
     *            the view change event
     */
    public default void openingView(ViewChangeEvent event) {
        UI.getCurrent().getPage().setTitle(getTranslation(event.getViewName()));
        Notification.show(
                String.format("%s %s", getTranslation(event.getViewName()),
                        getTranslation(I18n.OPENED)),
                Notification.Type.ASSISTIVE_NOTIFICATION);
        // The cast is safe as TabView is only added to TabNavigator
        // which ensures that the views are ComponentContainers
        Telemetry.entered((ComponentContainer) event.getOldView(),
                (ComponentContainer) this);
    }

    /**
     * Event received by the listener for attempted and executed view changes.
     */
    public static class ViewChangeEvent extends Component.Event {
        @Nullable
        private final TabView oldView;
        private final TabView newView;
        private final String viewName;

        /**
         * Create a new view change event.
         *
         * @param navigator
         *            TabNavigator that triggered the event, not null
         * @param oldView
         *            The view being deactivated, null if none
         * @param newView
         *            The view being activated, not null
         * @param viewName
         *            The name of the view being activated, not null
         */
        public ViewChangeEvent(TabNavigator navigator,
                @Nullable TabView oldView, TabView newView, String viewName) {
            super(navigator);
            this.oldView = oldView;
            this.newView = newView;
            this.viewName = viewName;
        }

        /**
         * Returns the navigator that triggered this event.
         *
         * @return Navigator (not null)
         */
        public TabNavigator getNavigator() {
            return (TabNavigator) getSource();
        }

        /**
         * Returns the view being deactivated.
         *
         * @return old View
         */
        public @Nullable TabView getOldView() {
            return oldView;
        }

        /**
         * Returns the view being activated.
         *
         * @return new View
         */
        public TabView getNewView() {
            return newView;
        }

        /**
         * Returns the view name of the view being activated.
         *
         * @return view name of the new View
         */
        public String getViewName() {
            return viewName;
        }
    }

    /**
     * ViewChangeEvent listener interface, can be implemented with Lambda or
     * anonymous inner class.
     */
    public interface ViewChangeListener extends ConnectorEventListener {
        Method VIEW_CHANGED_METHOD = ReflectTools.findMethod(
                ViewChangeListener.class, "viewChange", ViewChangeEvent.class);

        public void viewChange(ViewChangeEvent event);
    }
}
